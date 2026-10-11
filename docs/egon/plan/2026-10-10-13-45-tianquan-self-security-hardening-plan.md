# 天权授柄与鉴神自身安全收敛实施 Plan

| Field | Value |
| --- | --- |
| Document | `2026-10-10-13-45-tianquan-self-security-hardening-plan.md` |
| Template Version | `4` |
| Status | `Completed` |
| Created | `2026-10-10 13:45 Asia/Shanghai` |
| Updated | `2026-10-11 09:58 Asia/Shanghai` |
| Owner | `mario` |
| Repository | `Egon-COLA` |
| Scope | 授柄/鉴神自身及用户明确授权的共享包0.2.5发布元数据/构建发布；其他平台/模板接入后续 |
| Source Requirement | 用户批准自身Spec/Plan执行；明确授权0.2.5构建发布及仅两个自身host安装，其他平台/模板后续 |
| Baseline Revision | `main@3a82ea27652232c741b3366e94da8b3b36ea03bd`；工作树只有本轮未跟踪Spec文档 |
| Implements Spec | [自身安全收敛Spec](../spec/2026-10-10-12-36-tianquan-security-self-hardening.md) |
| Spec Status | `Accepted` |
| Spec Revision | `2026-10-10 14:03 Asia/Shanghai`；本轮用户批准及补齐RPC调用点 |
| Effective Specs | [主目标](../spec/2026-10-10-12-36-tianquan-security-self-hardening.md)；[OAuth/OIDC原合同](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md)；[当前代码修订（限指定段落）](../spec/2026-10-08-10-18-oauth-current-code-alignment.md)；[鉴神字段读/PEP合同](../spec/2026-08-17-09-37-rbac3-annotation-userdetails-field-authorization.md)；[Outbox/CQE技术合同](../spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

本Plan用11个独立验证、单次提交的逻辑Step实现已批准Spec。顺序是限定CORS/字段写guard、严格spool、跨节点receipt、真实观测、HTTP/RPC与SAS UserInfo接入、RBAC异常、自身配置、前端产物及交付门禁。66个唯一目标文件全部属于两个自身子平台，其中RPC interceptor是用户本轮明确批准补齐的现有调用点。
本轮只编写文档及Spec批准/关联/上述清单修订；以下伪代码、Maven/npm命令、RED/GREEN、提交与部署验收均为未来执行说明。没有修改生产/测试代码，没有启动项目、PG/Redis、浏览器、GUI、迁移、下载依赖、发布制品或提交。Plan状态Review，必须经用户审核后再进入执行。

## 2. Target Spec and Effective Design

### 2.1 Primary target

主目标为[自身安全收敛Spec](../spec/2026-10-10-12-36-tianquan-security-self-hardening.md)，状态Accepted，修订`2026-10-10 14:03 Asia/Shanghai`，代码基线`main@3a82ea27652232c741b3366e94da8b3b36ea03bd`。批准证据：用户“批准这份 Spec…先只修复授柄和鉴神自身的问题…暂时不要改代码”；随后选择“补齐 Spec 清单并纳入 Plan”，仅增加既有RPC interceptor的proof传递调用点。字段写guard已在上一轮由用户明确选择纳入。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [自身收敛](../spec/2026-10-10-12-36-tianquan-security-self-hardening.md) | Accepted / `2026-10-10 14:03 Asia/Shanghai` | §1–20 | 19项当前需求、接口/模型、65文件、28测试与阶段门禁 |
| Dependency | [OAuth/OIDC](../spec/2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md) | `Accepted` / `2026-09-23 10:51 Asia/Shanghai` | §7/9/15.1/15.2/16未改合同；§9/15.3/15.4本轮修正处依主目标 | 密码/issuer/PKCE/claims/revoke/key/迁移信任边界 |
| Dependency | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) | `Review` / `2026-10-09 17:37 Asia/Shanghai` | §6/9.4/9.5/11及已实施当前事实；§9/15技术执行依主目标 | 当前MP、原生consent、C10 USER scope及同源事务 |
| Dependency | [鉴神字段](../spec/2026-08-17-09-37-rbac3-annotation-userdetails-field-authorization.md) | `Accepted` / `2026-08-17 15:07 CST` | §7/9/10未改读权限、本地registry与method PEP；§3.2写排除仅主目标窄幅覆盖 | 保留旧三维PDP、READ输出和角色规则 |
| Dependency | [Outbox/CQE](../spec/2026-09-24-12-00-outbox-statemachine-cqe-integration.md) | `Implemented` / `2026-09-27 08:52 CST` | 技术生命周期、同源事务、持久事实与重试语义；不启business adapter | 原签发/撤销Outbox不可被usage MQ替换 |
上述前置Spec的metadata原样保留，尤其代码修订仍为Review，不把其既有执行授权追认成整份Accepted。当前用户批准的主目标已明确采纳指定依赖段落，其余段落只用于不回退边界；本Plan没有重启旧Plan或声明其全部运行通过。前置Spec中重复的REQ编号必须带文档前缀辨识；本Plan裸REQ-001–019均指主目标。

### 2.3 Superseded or excluded content

优先级：最新用户范围/批准→自身主目标点名修正→当前代码修订的已纳入合同→原OAuth/字段合同→Outbox技术合同。旧字段§3.2仅“写字段校验”排除项由当前显式平台能力取代，读serializer不变；原OAuth单issuer/固定RT/统一aud/JPA已被当前事实替代，不恢复。原更广泛Gateway/DDC/Edge/模板接入、JA全域JPA→MP、可选业务SSM、旧任务“仅写Spec”阶段限制，不作为此次实现任务；用户最新授权仅写Plan。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| REQ-001 | 主目标§4；详细§7–16 | 两子平台自身先闭合 | 变更只在§8清单；其他平台/模板不改；未通过exit gate不得宣称后续接入完成 | Step 1,2,3,4,5,6,7,8,9,10,11 |
| REQ-002 | 主目标§4；详细§7–16 | 保留已有合理安全机制 | 原协议/claims/PKCE/redirect/密码/撤销/tenant/方法PEP/C10回归不退化 | Step 1,2,4,5,6,8,9,10,11 |
| REQ-003 | 主目标§4；详细§7–16 | OAuth CORS仅明确白名单与四原生资源 | 合法preflight成功；非法origin/方法/header无允许头；login/consent/authorize/Admin不因此开放跨域 | Step 1 |
| REQ-004 | 主目标§4；详细§7–16 | 自身安全异常按既有族处理 | 缺认证401、授权拒绝403、必需状态/审计不可用503；过滤器错误不依赖ControllerAdvice；OAuth协议错误不换业务wrapper | Step 6,8,9 |
| REQ-005 | 主目标§4；详细§7–16 | 每次自身受保护Token使用决策有审计（按决策阶段唯一） | 允许、拒绝、依赖失败都有唯一usage event；未知JWT不伪造issuer/subject；public health/preflight不计使用 | Step 6,8,9 |
| REQ-006 | 主目标§4；详细§7–16 | 审计durability是业务放行前置 | 本地force/目录持久性与MQ确认成功后才能进入业务；仅XADD或失败双介质不能放行；返回503且业务零调用 | Step 3,4,6,8,10 |
| REQ-007 | 主目标§4；详细§7–16 | 生产者只按可验证持久确认清理 | 远端consumer DB提交→receipt→XACK；producer验证eventId/fingerprint/MAC后删除自身文件；错误/缺失/DEAD确认不删除 | Step 4 |
| REQ-008 | 主目标§4；详细§7–16 | 重放、崩溃、重复、过期有可判定行为 | 同eventId同内容幂等；ACK失效重新投递；不重建180天前已清理审计；DEAD本地隔离保留；有界扫描不饥饿 | Step 3,4 |
| REQ-009 | 主目标§4；详细§7–16 | JWT验证耗时来自真实验签 | 成功/失败真实密码学路径都有timer；不含Redis/业务耗时；预检未验签不冒充timer样本 | Step 5,6,7,8 |
| REQ-010 | 主目标§4；详细§7–16 | JWKS与SERVICE Token缓存指标准确 | 分别记录实际HIT/MISS/EXPIRED及Token acquire duration；不将manager map命中当JWK命中；不改变授权key | Step 5,7 |
| REQ-011 | 主目标§4；详细§7–16 | 授权拒绝/不可用可观察 | 认证和RBAC结果分开计数，有限reason category，无tenant/user/client/jti/scope/URL标签 | Step 5,7,9 |
| REQ-012 | 主目标§4；详细§7–16 | 显式字段写闸门复用既有PDP | 对服务器确定的提交字段集合，overall ALLOW且每项WRITE才通过；READ/MASKED_READ/NONE/缺字段拒绝；旧读决策不变 | Step 2,9 |
| REQ-013 | 主目标§4；详细§7–16 | 写闸门不信任前端/不自动改业务输入 | 无caller tenant/subject/role字段；null也视为写；无通用反序列化/自动DTO丢字段/隐式授权接口；其他平台接口暂不接入 | Step 2 |
| REQ-014 | 主目标§4；详细§7–16 | 全层约束与已有Java/组件规范 | §6.2/10/20逐项满足；手工验证只补非代理schema；MapStruct/Egon转换、具名qualified Lombok注入 | Step 1,2,3,4,5,6,7,8,9,10,11 |
| REQ-015 | 主目标§4；详细§7–16 | 配置和可选能力隔离 | base/local核心键同构，offline profile显式关闭web observation；只有自身Admin启用；缺安全依赖启动失败，其他host默认不新增审计副作用 | Step 4,6,7,9,10 |
| REQ-016 | 主目标§4；详细§7–16 | HTTPS可信边界保持且可验证 | public issuer/资源/service endpoints/Redis PRIMARY按原HTTPS/TLS合同校验；Cookie原CSRF不关；不采信任意Forwarded身份头 | Step 1,6,10 |
| REQ-017 | 主目标§4；详细§7–16 | 自身前端使用一致的已有共享产物 | 按PC-014构建发布当前源码一致的0.2.5制品，两个自身package常规typecheck/test通过；不以临时路径别名替代 | Step 11 |
| REQ-018 | 主目标§4；详细§7–16 | 无关系模型/历史DDL改动 | 原SQL/manifest/业务表/唯一键/tenant事务字节与语义不变；无新migration/DDL/generator动作 | Step 1,2,3,4,5,6,7,8,9,10,11 |
| REQ-019 | 主目标§4；详细§7–16 | “可供接入”有明确门禁 | §16逐项证明本平台source、受控行为、真实部署验收或明确Runtime unverified；不能把Spec/Mock检查判生产Ready | Step 10,11 |
验收逐条保持：四OPTIONS 200/bodyless与403技术text；401/403/503原族；完整Valid才产可信refs，验证失败统一fallback；file与目录force/atomic+MQ先于业务；receipt在原持久事实后写、producer验指纹MAC再清理；固定九类信号；显式submittedFields含null而省略不计；三个PDP key不扩展；所有profile同核心键；历史SQL/manifest、旧机制与其他平台不变。详见主目标API-001–004、INTERNAL-001–007、EVENT-001、JOB-001、TEST-001–028。

前置合同仅按§2列出的段落保留：OAuth issuer/RS256/aud/type/scope/撤销、身份tenant finally、PKCE/精确redirect、CLIENT_CREDENTIALS近过期重新申请、METHOD/READ字段PEP、原MP/Outbox同连接事务。以下代码修订ID为跨文档跟踪，不增加本轮实施范围；超出自身范围的条目只验证无改动，既有运行缺口维持Runtime unverified。

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| ALIGN:REQ-020 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | 认证四种目录事实统一MP | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-021 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | 认证写与组件Outbox同库同事务 | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-022 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | 导入保留历史与可恢复性 | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-023 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | MP装配及manifest family遵当前真实API | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-024 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | 保留Tianshu两层信任 | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-025 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | 保留玉衡实际数据面与流式行为 | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-026 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | 补齐现有Grant读合同 | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-027 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | 文档与依赖方向同步最新基线 | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-028 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | 加密授权/令牌载荷静态存储 | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-029 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | 管理API把已验证realm显式传至MP服务 | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-030 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Signing key管理使用issuer隔离的加密keyring | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-031 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | 保留Resource Server bounded collection/read semantics | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-032 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Resource/CLIENT_CREDENTIALS Grant Redis投影通过提交后Outbox交付 | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-033 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Production redirect URIs remain exact HTTPS; local bootstrap alone permits exact IPv4 loopback HTTP | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-034 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Connect SAS and identity security changes to durable revocation, audit and rotation runtime | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-035 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Provide version-guarded repository deletion for policy-expired Audit and Revocation rows | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-036 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Protect BFF session and AuthorizedClient state at rest | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-037 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Make BFF logout and remote token-family revocation recoverable | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-038 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Publish the BFF session operations through the existing WebFlux OpenAPI contract | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-039 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Bind the Edge route table to the pinned Spring Cloud Gateway property prefix | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-040 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Compile Edge constructor injection with the existing repository Lombok contract | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-041 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Serialize BFF refresh-token rotation across Edge instances | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-042 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Make API-015 report only a currently valid BFF session | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-043 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Migrate Portal/four Admin consumers to passwordless OIDC through the Edge BFF and preserve high-risk role reauthentication | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-044 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Provide an explicitly invoked, offline and restart-safe identity/OAuth data migration | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-045 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Complete legacy protocol retirement and direct consumers | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-046 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Production tenant SERVICE registration selection | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-047 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Complete native SAS consent and personnel OIDC roundtrip | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |
| ALIGN:REQ-048 | [修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4（限§2所列范围） | Preserve explicit USER business Grant scopes through every existing authority handoff | 现有合同保留；不在本轮重做或宣称运行通过 | Step11差异边界检查；自身必要回归见§8 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

先建立可独立证明的安全行为，随后将技术投递路径闭合到原持久事实，再开启真实入口。Step4必须先部署兼容consumer，再允许Step6/8/9的strict producer实际放量；源码配置到Step10才启用自身host。Core port只向SDK/host供类型，Admin适配已有Actuator。最终无法保证原子发布/目录持久性的环境明确不ready，不以降级仅MQ绕过。无DDL、generator、proto、依赖升级、公共页面/业务接口；三个POM只按用户批准补齐现版本编译能力。

### 4.2 Test-first strategy

每Step先证明缺失行为再最小GREEN。新类型/overload导致编译前置时，在同一Step按下列精确文件顺序建立未注册的、仅委托原行为的签名；紧随其后运行行为断言进入RED，再回到相同文件完成行为。这些前置签名/临时委托不能单独提交、不能开启生产Bean，也不能把编译失败算RED。Step2/3/5/6/7的重复File项只表示同Step两次编辑同一文件；库存只列该路径一次。最终验证要求没有签名占位/未实现分支。前端缺artifact是执行输入阻塞，必须另行记载，不能充当RED或以源码alias取代GREEN。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None（执行串行） | 其他Step写入 | 限定原生 OAuth CORS 并同步生成合同；共享文件分段见§5/7 |
| Step 2 | None | None（执行串行） | 其他Step写入 | 提供显式字段写权限闸门；共享文件分段见§5/7 |
| Step 3 | None | None（执行串行） | 其他Step写入 | 把严格审计持久化设为放行前置；共享文件分段见§5/7 |
| Step 4 | Step 3 | None（执行串行） | 其他Step写入 | 闭合跨节点审计确认与有界重放；共享文件分段见§5/7 |
| Step 5 | None | None（执行串行） | 其他Step写入 | 从真实验签和缓存分支发出安全观测；共享文件分段见§5/7 |
| Step 6 | Step 3,Step 4,Step 5 | None（执行串行） | 其他Step写入 | 为 HTTP 与 RPC 验证结果接上严格使用审计；共享文件分段见§5/7 |
| Step 7 | Step 5,Step 6 | None（执行串行） | 其他Step写入 | 把 Core 观测接到两个 Admin 的现有指标设施；共享文件分段见§5/7 |
| Step 8 | Step 3,Step 4,Step 6,Step 7 | None（执行串行） | 其他Step写入 | 在原生 UserInfo 返回前完成使用事实投递；共享文件分段见§5/7 |
| Step 9 | Step 2,Step 6,Step 7 | None（执行串行） | 其他Step写入 | 保留 RBAC 可信观察信息并闭合安全异常；共享文件分段见§5/7 |
| Step 10 | Step 1,Step 2,Step 4,Step 6,Step 7,Step 8,Step 9 | None（执行串行） | 其他Step写入 | 只在两个自身 Admin 启用并验证配置与 TLS 门禁；共享文件分段见§5/7 |
| Step 11 | Step 10 | None（执行串行） | 其他Step写入 | 校验自身前端制品并完成范围与回归交付门禁；共享文件分段见§5/7 |
Step1/2/3/5逻辑上可独立，但不要求或授权并行agent。统一串行Maven和提交，避免共享protobuf/generated目录与自动装配文件并发写。每个Step依赖其前置Step已验证并提交的新HEAD；开始前再次确认自身路径没有未归属改动。

### 4.4 Commit boundaries

11个Step分别是11个逻辑任务；每任务最多一个精确路径commit。先RED/GREEN和适用MC全部验证，再只git add该Step完整Commit paths并审查cached diff，commit后记录新HEAD，才进入下一Step；不批量攒多个Step，不产生空/占位commit。工作树已有Spec/Plan和其他新改动不自动纳入源码提交；当前文档任务不提交。复用同一源码文件的理由：Step1 CORS/Step9错误链、Step3 strict/Step4确认、Step5真实计时/Step6可信capture、Step7 adapter/Step10TLS等为独立可验证符号切片，已完成前置内容不可回退。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| 四资源CORS | §7.0/9 API001–004 Modify | 现IdpSecurityConfig只注册/oauth2/** | 复用DefaultCorsProcessor，限定Decorator与matcher | 0DB/0新Controller；浏览器既有preflight | Implement |
| field guard/Command/converter | §7.0/10/13 Add | AuthorizationService.FieldPolicyRequest仅三key；WRITE现有 | 复用一次decideFields和原FieldPolicyDecision | 0新网络/0写；仅一个Set与显式PEP | Implement |
| strict spool/receipt | §7/9 INTERNAL002/003/006 EVENT/JOB Add | append磁盘失败可broker fallback；consumer删自身目录 | 复用原frame/Streams/MP persist/job | 每事实fsync+MQ；短期Rediskey/有界扫描，无新库 | Implement |
| Core port与host adapter | §7.0/13 Add | SDK无Micrometer直接依赖；两Admin已有Actuator | JDK port+现有MeterRegistry；原缓存真实branch | 0新依赖/网络/cache；固定9策略 | Implement |
| proof/context与RPC bridge | §9 INTERNAL007/10 Add；本轮批准补清单 | 原principal无issuer；final USER/SERVICE wrapper委托IdpJwtVerifier | configured verifier组合+已有Context overload | 0新decode/ThreadLocal/接口类型；仅secret-free proof | Implement |
| 协议/方法/读serializer/缓存/Outbox | §3/7 Keep | 既有SAS/PEP/typed state/cache/同源事务 | 原代码直接复用 | 只有边界回归 | Already exists |
| fetch-then-forward/自动DTO过滤/新库/全域迁移 | §3.2/7.0 Remove | 现有PDP与技术MQ能满足此次scope | server context/显式guard/旧持久化 | 排除新state/调用/表/层/库 | Excluded |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 限定原生 OAuth CORS 并同步生成合同 | REQ-001,REQ-002,REQ-003,REQ-014,REQ-016,REQ-018 | 现有真实安全链不为四tenant资源产生正确允许头；缺header的OPTIONS可能进入错误链；OAS没有对应四OPTIONS | 当前基线及同Step签名前置 | 四资源协商真实链/OAS一致；Cookie保护与Admin同源保持；审计仍原开关 | 后续精确调用者见File blocks | Step 1 |
| 提供显式字段写权限闸门 | REQ-001,REQ-002,REQ-012,REQ-013,REQ-014,REQ-018 | 只委托旧PDP的编译前置闸门会把overall ALLOW下的READ或缺字段当可写；断言拒绝和businessSpy零调用失败 | 当前基线及同Step签名前置 | 平台guard单次PDP+全WRITE成立，三维旧请求/读serializer兼容；没有业务接口自动接入 | 后续精确调用者见File blocks | Step 2 |
| 把严格审计持久化设为放行前置 | REQ-001,REQ-006,REQ-008,REQ-014,REQ-018 | appendStrict编译前置仅委托旧append：stage/fsync失败仍可broker-only返回，atomic fallback不拒绝 | 当前基线及同Step签名前置 | strict stage/force/atomic/dir durability+MQ失败行为受控；旧append未被全host隐式开启 | 后续精确调用者见File blocks | Step 3 |
| 闭合跨节点审计确认与有界重放 | REQ-001,REQ-002,REQ-006,REQ-007,REQ-008,REQ-014,REQ-015,REQ-018 | 旧consumer先XACK且没有receipt；旧acknowledge凭eventId删文件，旧replay跳过.sent；双目录/伪确认/timeout断言失败 | Step 3 | consumer原persist完成→receipt→XACK；sender核对后清本目录，DEAD/EXPIRED与循环cursor可恢复；self producer尚未启用 | 后续精确调用者见File blocks | Step 4 |
| 从真实验签和缓存分支发出安全观测 | REQ-001,REQ-002,REQ-009,REQ-010,REQ-011,REQ-014,REQ-018 | 编译前置只增加兼容观察overload并委托原流程，真实验签/cache执行后recording port样本仍为零；不是编译失败 | 当前基线及同Step签名前置 | Core纯port与真实密码学/JWK/SERVICE信号可验证；旧构造/factory/key/单飞兼容；host接线尚未启用 | 后续精确调用者见File blocks | Step 5 |
| 为 HTTP 与 RPC 验证结果接上严格使用审计 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-015,REQ-016,REQ-018 | 编译前置observer/filter只委托原验证/chain，真实HTTP/RPC调用仍无usage帧/MQ，handoff-before-return与proof恢复断言失败 | Step 3,Step 4,Step 5 | configured原verifier覆盖USER/SERVICE/combined与RPC，失败匿名fallback，Valid携带secret-free proof；自身开关仍待配置启用 | 后续精确调用者见File blocks | Step 6 |
| 把 Core 观测接到两个 Admin 的现有指标设施 | REQ-001,REQ-009,REQ-010,REQ-011,REQ-014,REQ-015,REQ-018 | adapter编译前置为空观察/配置仍不启用；调用实际verifier/cache记录不到对应Timer/Counter，ContextRunner缺开启必需依赖断言失败 | Step 5,Step 6 | 两host固定receiver的真实registry adapter与具名Supplier可装配；缺能力不noop，SDK无Micrometer新依赖 | 后续精确调用者见File blocks | Step 7 |
| 在原生 UserInfo 返回前完成使用事实投递 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-018 | 旧真实UserInfo Strategy完成验签/撤销后直接响应，producer调用为零；handoff失败仍返回成功 | Step 3,Step 4,Step 6,Step 7 | SAS UserInfo原验证一次，usage先响应；与token签发/撤销Outbox及原OAuth错误族保持兼容 | 后续精确调用者见File blocks | Step 8 |
| 保留 RBAC 可信观察信息并闭合安全异常 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-011,REQ-012,REQ-014,REQ-015,REQ-018 | 旧RBAC桥不保留proof、PDP无拒绝usage/专用计数，真实JA两chain没有明确错误族；401/403/503/先审计及不同stage ID断言失败 | Step 2,Step 6,Step 7 | 五PDP方法结果/异常原样观察，HTTP/RPC拒绝在返回/渲染前留事实；401/403/503及no-store自有链明确 | 后续精确调用者见File blocks | Step 9 |
| 只在两个自身 Admin 启用并验证配置与 TLS 门禁 | REQ-001,REQ-002,REQ-006,REQ-014,REQ-015,REQ-016,REQ-018,REQ-019 | 现有profile无usage/delivery一致键且RPC/Tianshu生产TLS默认false；受控binder/parity/TLS拒绝与缺依赖启动失败断言失败 | Step 1,Step 2,Step 4,Step 6,Step 7,Step 8,Step 9 | self base/local开启、offline/test显式关闭或override；四种部署/例外条件可验证，其他host默认关闭；runtime仍未验收 | 后续精确调用者见File blocks | Step 10 |
| 校验自身前端制品并完成范围与回归交付门禁 | REQ-001,REQ-002,REQ-014,REQ-017,REQ-018,REQ-019 | 可加载旧产物的真实export/login/logout/stepUp契约断言失败；包未安装或tsc编译错误只算环境阻塞，不能作为RED | Step 10 | 两个真实安装产物的普通typecheck/test与既有锁定约定一致；受控回归和源范围/SQL不变有记录；Runtime unverified另列 | 后续精确调用者见File blocks | Step 11 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | 两Admin实际controller/service/repository/dao与JS/SS技术包；JA AdminLayerBoundaryTest | 主目标§6.1传统三层 | 唯一选择传统三层+现有SDK，未选择Archetype；保持Core→SDK→Admin依赖、Controller不摸DAO | 全部；MC-ARCH-001 |
| Reuse/capability | 现有POM及模块Common/Validation/MapStruct/Actuator/Redisson/SAS | 主目标§6.1 | 先复用，仅用户批准的3个自身POM补齐编译API/processor；无新框架/层 | MC-REUSE-001/MC-DEP-001 |
| Naming/model/validation/conversion | Common BaseForwardConverter、ValidationUtils真实实例API；原immutable Event/Decision | 主目标§6.2/10 | 普通载体完整Lombok，Proof值对象record；两个generated mapper；WriteFields与default/cascade显式交接 | Step2/4/6；MC-NAME-001/MC-MODEL-001/MC-VALID-001/MC-CONVERT-001 |
| Bean/logging/util/JSON/time/config | Shoubing三模块lombok.config已有copyable Qualifier，ResourceServerServiceImpl用defaultValidator；JA parent缺配置 | 主目标§6.2/15 | 业务@Slf4j、具名Bean、RequiredArgsConstructor final qualified；JDK/Boot Jackson/Instant Duration；JA新增copyable配置 | Step2/6/7/10；MC-BEAN-001/MC-LOG-001/MC-UTIL-001/MC-JSON-001/MC-TIME-001/MC-CONFIG-001 |
| Business variation/pattern | 原SAS事务Decorator、技术spool状态、真实cache/验证分支 | 主目标§13 | Decorator保留验证/PDP语义；Observer/Adapter隔离host；固定method-reference Strategy；原技术State；固定字段集合Simple直接guard | Step4–9；MC-PATTERN-001 |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| 协议与CORS | Spring SAS/Security/Web/Boot | SA AuthorizationServerConfiguration/IdpSecurityConfig、SA POM | 协议足够；4路径matcher/单值header限制缺失 | 保留SAS+DefaultCorsProcessor | 无新依赖；限定processor | Step1；MC-REUSE-001/MC-DEP-001 |
| 校验/投影 | Validation Starter/Jakarta；Common Core/MapStruct | egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/validation/ValidationUtils.java与converter/BaseForwardConverter.java | 现成API充分；ValidationUtils是实例facade | 复用new ValidationUtils(defaultValidator).validate，注解为规则来源 | 无新依赖/Utils类；2个真实mapper | Step2/4/6；MC-VALID-001/MC-CONVERT-001 |
| 审计MQ/恢复 | 现TokenUsageAuditComponent/Redisson/Outbox | SS audit包、SA OAuthAuditConsumer、O/pom.xml与现技术生命周期 | 原MQ/同源persist可复用；remote确认缺 | 扩原strict/receipt/job，Outbox业务adapter关闭 | 无新库/cache/job/DB表 | Step3/4/6/8/9；MC-ARCH-001/MC-PATTERN-001 |
| 指标 | Boot Actuator/MeterRegistry；Core本地port候选 | SA/JA POM已有Actuator；SS POM无Micrometer直接依赖；RAG recorder仅LLM领域 | host可用；SDK精确分支没有通用OAuth port | 纯JDK port+已有host registry | 无新依赖；必要port/adapters | Step5/7；MC-REUSE-001/MC-DEP-001 |
| 字段写 | AuthorizationService/FieldPolicyDecision/CurrentRbac3User | JS authorization/field/security原树 | PDP充分但未检查submitted WRITE集合 | Simple guard组合现PDP；不加Advice/引擎 | 无新接口/依赖，显式Command/mapper/component | Step2；MC-PATTERN-001 |
| SQL与DDL | Common MP/受管DDL/原Outbox Store | OAuthAuditRepository及原db/egon-mp SQL/manifest；EgonColaPostgreDdlRunner | 已有事实表/事务充分；新receipt是Redis副本 | 原PO/UK/query/历史DDL字节不变 | 无DDL/generator/新迁移；JA历史JPA本轮不迁 | Step4/11；MC-MODEL-001/MC-SCOPE-001 |
| 前端 | 现npm/Vitest/tsc与共享制品 | SW/JW package.json；egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.ts | 源码0.2.4契约已正确；本机产物是执行输入缺口 | 原range/alias下既有artifact校验 | 零新增包/发布/共享源改动 | Step11；MC-DEP-001/MC-TEST-001 |

### 4.8 User-mandated Java Rule Implementation Matrix

以下十条原文及编号不改写：

```text
1 类名规范，必须以 java的pojo规范命名。以dao po bo vo dto query command event等结尾
2 每层之间必须被 springboot-validation 校验，复用对象使用分组校验；基于原生和自定义约束注解、@Valid、@Validated 及 ConstraintValidator 扩展实现。ValidationUtils 只承担通用手工校验，不承载全部业务校验；电话号码复用 libphonenumber。
3 Java POJO 默认使用 class，注解为 @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true)，按构造目标选择 @Builder 或 @SuperBuilder；父类状态参与相等性时使用 @EqualsAndHashCode(callSuper = true)。只有不可变 value object 可以使用 record，并豁免 class 的 Lombok 规范；普通实体不能因简单而使用 record。使用 MapStruct、MapStructPlus 和 egon-cola-component-common-core 的 BaseConverter（双向）或 BaseForwardConverter（不可逆投影）进行转换。
4业务类必须使用@Slf4j注解注入log对象。如果业务类被spring管理，必须指定名称，如果是单例的情况下，参考@Service("userService")。如果需要依赖注入，必须@RequiredArgsConstructor进行修饰，不要代码中写。且属性必须被@qualify修饰。
5 工具类只允许使用jdk原生、Apache Commons(commons-lang3、commons-collections4、commons-io、commons-text、commons-codec、commons-beanutils)、Guava。针对Tika按需引入。
6 JSON 使用 Spring Boot Jackson；持久化枚举值使用 @EnumValue，向前端输出的枚举值使用 @JsonValue，禁止 ordinal 作为业务编码。
7 springboot 多环境配置文件，必须保持配置一致，但值不一定一致。
9 复杂业务必须引入设计模式，不允许硬编码
10 日期相关的必须使用java.time下的实体类，不允许使用java.util下的
11 只允许现有三层结构或当前 egon-cola-archetypes 的 DDD 结构，三层结构保持现状。必须尽量复用 egon-cola-components；持久化模块必须使用 Egon COLA MP Starter，DDL 统一由 egon-mp-sdj-ext-starter 分布式管理。业务唯一键必须组合业务列与 deletedAt（数据库 deleted_at），并验证 NULL、租户及重复软删除语义。采用 CQE（Command Query Event）；Event 必须经 egon-cola-component-transactional-outbox-starter 或 MQ 中间件投递。
```

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | 主目标§6.2/8/10 | BO/DTO/Command/Component/Converter roles | §5完整74路径→§7顺序 | 精确语义suffix，复用原类型不造Data/Info/Param | 库存与Java编译/命名审阅 | 1–10 | PASS（计划审阅） |
| Rule 2 | 主目标§6.2/10.3.1 | 原defaultValidator、Common实例facade、PDP三key | Command/metadata→mapper→guard；Event/receipt→producer/consumer | WriteFields、Default、Valid cascade，规则在annotations/state owner；本轮无电话字段/libphonenumber N/A | 真实Validator/generated Mapper/负例与HV000151测试 | 2/3/4/6/7/10 | PASS（计划审阅） |
| Rule 3 | 主目标§10 | 原immutable Event/Decision与MapStruct | 普通BO/Props/DTO/Command→两个converter→消费者 | Data/NoArgs/AllArgs/Accessors+Builder；根不callSuper；proof record真immutable；BaseForwardConverter | 生成Mapper编译、集合不可变/JSON兼容断言 | 2/4/5/6 | PASS（计划审阅） |
| Rule 4 | 主目标§6.2/7/15 | 具名factory/defaultValidator；SS/SA/SC lombok config | JA parent lombok.config→新业务Bean→auto config | Slf4j、named bean、RequiredArgsConstructor final Qualifier，旧技术API构造compat不改 | context/生成构造参数/无secret日志审阅 | 2/4–10 | PASS（计划审阅） |
| Rule 5 | 主目标§6.2/15 | FileChannel/Mac/MessageDigest/URI与Common | 原codec/新增技术参与者 | 仅JDK/既有准Commons/Guava；无新Utils/工具库/Tika需求 | imports/POM范围检查 | 1–10 | PASS（计划审阅） |
| Rule 6 | 主目标§9/10 | Boot Jackson/原Event schema1 | DTO/proof/Valid→strict reader/mapper→tests | JsonIgnore/ToString.Exclude、6字段strict unknown、原11字段/普通Valid JSON不变；无新增持久/前端enum | wire/OAS/未知字段/敏感输出测试 | 1/2/4/6/8/9 | PASS（计划审阅） |
| Rule 7 | 主目标§15 | SA base/local/offline、JA base/local/local-it | Properties→两host配置校验→6profile files | 同核心叶子键，值可不同，offline/test显式关；原TLS键同构 | TEST026 YAML加载/绑定/key parity | 4/6/7/10 | PASS（计划审阅） |
| Rule 9 | 主目标§13 | 原Decorator/State、fixed signal变体 | 状态/观察port→策略参与者→原链 | Complex使用Decorator/Adapter/Observer/fixed Strategy/原State；field固定集合Simple | 真实分支/失败/metrics与state证明 | 4–9 | PASS（计划审阅） |
| Rule 10 | 主目标§10/11/15 | Clock/Instant/Duration | BO/DTO/配置→codec/cache→Timer | UTC Instant ISO、Duration有界、elapsed nano monotonic；无Date/Calendar/SimpleDateFormat | Clock/expiry/JSON及import检查 | 3–10 | PASS（计划审阅） |
| Rule 11 | 主目标§6.1/8/11 | 传统树/AdminLayerBoundaryTest；SA MP/Outbox | 每个库存路径在原owner，零新层/表/DDL | 原tenant/soft-delete UK不改；Event真实MQ；旧签发/撤销Outbox同源事务保持 | 架构、git SQL字节差异与CQE顺序证明 | Every Step | PASS（计划审阅） |

## 5. Change File Tree

下表是完整且唯一的目标树/库存，以完整仓库相对路径展开Spec的别名。CREATE路径当前不存在，MODIFY路径当前存在，全部已核对；无DELETE/RENAME/GENERATED。同文件跨Step仅允许表列出的精确技术切片，章7是执行顺序；Tests按所属Step逐步扩充，不新增未批准测试路径。

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/observability/IdpSecurityMetricsRecorder.java` | 当前不存在；同模块已存在POM/邻近原包 | record | 纯JDK指标port，无Micrometer/HTTP/身份标签 | 5 | REQ-001,REQ-002,REQ-009,REQ-010,REQ-011,REQ-014,REQ-018 | Step 5选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/observability/SecurityMetricObservationBO.java` | 当前不存在；同模块已存在POM/邻近原包 | signal/result/duration/value | 有界技术观测值，默认组校验 | 5 | REQ-001,REQ-002,REQ-009,REQ-010,REQ-011,REQ-014,REQ-018 | Step 5选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java` | 当前存在；verifyAny/verifyMultiIssuer观察overload | verifyAny/verifyMultiIssuer观察overload | 原流程一次verify；将真实可信事实与stage计时交观察者 | 5,6 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-010,REQ-011,REQ-014,REQ-015,REQ-016,REQ-018 | Step 5,6选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java` | 当前存在；authenticate观察overload/BoundedJwkSetCache | authenticate观察overload/BoundedJwkSetCache | 真实JWT/JWK分支观察，保留2/3参构造和旧resolver接口（Edge调用保持） | 5 | REQ-001,REQ-002,REQ-009,REQ-010,REQ-011,REQ-014,REQ-018 | Step 5选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpBearerAuthenticationFilter.java` | 当前存在；create/请求观察回调 | create/请求观察回调 | 原身份安装/清理保留；按opt-in把本请求可信事实交usage filter | 6 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-015,REQ-016,REQ-018 | Step 6选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpSecurityObservationFilter.java` | 当前不存在；同模块已存在POM/邻近原包 | doFilterInternal | 使用审计/拒绝观察的真实Servlet执行点；只在自身enabled链接入 | 6 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-015,REQ-016,REQ-018 | Step 6选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpObservedVerificationComponent.java` | 当前不存在；同模块已存在POM/邻近原包 | observe | 包装自身managed USER/SERVICE verifier与RPC返回前审计；复用原验证，不新decode | 6,7 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-015,REQ-016,REQ-018 | Step 6,7选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/TokenAuditProofBO.java` | 当前不存在；同模块已存在POM/邻近原包 | immutable verified proof | 可信issuer/resource及脱敏refs，允许跨SecurityContext/gRPC当前调用保留；不含JWT | 6 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-015,REQ-016,REQ-018 | Step 6选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/AccessTokenVerification.java` | 当前存在；Valid可选proof | Valid可选proof | 保留原一参/两参构造/字段；新proof为JsonIgnore immutable metadata，legacy/disabled可为null | 6 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-015,REQ-016,REQ-018 | Step 6选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/rpc/IdpRpcSecurityContext.java` | 当前存在；可选proof overload | 可选proof overload | 保留原with/run方法；当前gRPC Context持有secret-free proof并finally恢复，不新增ThreadLocal | 6 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-015,REQ-016,REQ-018 | Step 6选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/security/Rbac3BearerAuthenticationFilter.java` | 当前存在；bridge/拒绝render前callback | bridge/拒绝render前callback | 在替换Authentication时保留已验证audit proof；RBAC拒绝先记录再写response | 9 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-011,REQ-012,REQ-014,REQ-015,REQ-018 | Step 9选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TransactionalOAuthAuthenticationProvider.java` | 当前存在；原UserInfo Strategy/Decorator观察点 | 原UserInfo Strategy/Decorator观察点 | 原签名/授权/撤销成功后，返回UserInfo之前记录Token使用；不重复JWT验证或改业务结果 | 8 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-018 | Step 8选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/TokenAuditContextBO.java` | 当前不存在；同模块已存在POM/邻近原包 | request context | 经验证BO+server-owned fallback/resource/result/clock，禁止JSON输出raw JWT | 6 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-015,REQ-016,REQ-018 | Step 6选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/converter/TokenUsageAuditEventConverter.java` | 当前不存在；同模块已存在POM/邻近原包 | toTarget | MapStruct+BaseForwardConverter投影到原11字段Event | 6,8 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-015,REQ-016,REQ-018 | Step 6,8选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java` | 当前存在；appendStrict/reference/confirm/replayPending | appendStrict/reference/confirm/replayPending | 原spool/Streams增强strict持久/receipt/有界重放，不删远端目录 | 3,4,6,7 | REQ-001,REQ-002,REQ-006,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011,REQ-014,REQ-015,REQ-018 | Step 3,4,6,7选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/domain/dto/TokenUsageAuditReceiptDTO.java` | 当前不存在；同模块已存在POM/邻近原包 | receipt schema1 | eventId/指纹/outcome/时间/MAC的严格技术确认 | 4 | REQ-001,REQ-002,REQ-006,REQ-007,REQ-008,REQ-014,REQ-015,REQ-018 | Step 4选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/OAuthUsageObservationProperties.java` | 当前不存在；同模块已存在POM/邻近原包 | enabled/fallbackIssuer | opt-in与接收方匿名失败审计authority | 6 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-015,REQ-016,REQ-018 | Step 6选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/AuditDeliveryProperties.java` | 当前不存在；同模块已存在POM/邻近原包 | TTL/timeout/batch | 有界确认/重放参数；原OAuthAuditProperties四字段不改 | 4 | REQ-001,REQ-002,REQ-006,REQ-007,REQ-008,REQ-014,REQ-015,REQ-018 | Step 4选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2Client.java` | 当前存在；authorize/renew观察overload | authorize/renew观察overload | 真实SERVICE缓存/重获计时，原cache key/single-flight/factory不变 | 5 | REQ-001,REQ-002,REQ-009,REQ-010,REQ-011,REQ-014,REQ-018 | Step 5选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java` | 当前存在；观察/producer组件Beans | 观察/producer组件Beans | 显式named/qualified装配，disabled不增加依赖；自身开启缺能力失败 | 4,5,6,7 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011,REQ-014,REQ-015,REQ-016,REQ-018 | Step 4,5,6,7选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumer.java` | 当前存在；process/parse/receipt | process/parse/receipt | persist返回后签名receipt再XACK；DEAD/EXPIRED区别；停止消费者代删sender目录 | 4 | REQ-001,REQ-002,REQ-006,REQ-007,REQ-008,REQ-014,REQ-015,REQ-018 | Step 4选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/IdpSecurityConfig.java` | 当前存在；CORS/自有观察filter/错误族 | CORS/自有观察filter/错误族 | 保留自身Bearer链，复用错误响应；CORS不开放Admin | 1,9 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-011,REQ-012,REQ-014,REQ-015,REQ-016,REQ-018 | Step 1,9选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java` | 当前存在；protocol matcher/cors object processor | protocol matcher/cors object processor | 将四技术preflight纳入同原生链，Cookie CSRF及协议Provider保持 | 1,8 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-016,REQ-018 | Step 1,8选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthCorsProperties.java` | 当前不存在；同模块已存在POM/邻近原包 | allowedOrigins | 校验已有白名单key，不创造自动反射/通配符 | 1 | REQ-001,REQ-002,REQ-003,REQ-014,REQ-016,REQ-018 | Step 1选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthNativeCorsProcessor.java` | 当前不存在；同模块已存在POM/邻近原包 | processRequest | Decorator复用DefaultCorsProcessor，仅限制四资源preflight单值header/方法 | 1 | REQ-001,REQ-002,REQ-003,REQ-014,REQ-016,REQ-018 | Step 1选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiConfiguration.java` | 当前存在；四options operations | 四options operations | 现有Customizer记录200/403/no body；原协议schema保留 | 1 | REQ-001,REQ-002,REQ-003,REQ-014,REQ-016,REQ-018 | Step 1选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityMetricsComponent.java` | 当前不存在；同模块已存在POM/邻近原包 | record | Core port→现有Micrometer，固定SHOUBING角色 | 7 | REQ-001,REQ-009,REQ-010,REQ-011,REQ-014,REQ-015,REQ-018 | Step 7选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityObservationConfiguration.java` | 当前不存在；同模块已存在POM/邻近原包 | metrics/observer/readiness Beans | 复用Boot primary/unique registry，具名Supplier解决qualified依赖 | 7,10 | REQ-001,REQ-002,REQ-006,REQ-009,REQ-010,REQ-011,REQ-014,REQ-015,REQ-016,REQ-018,REQ-019 | Step 7,10选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/authorization/AuthorizationService.java` | 当前存在；nested FieldPolicyRequest schema annotations | nested FieldPolicyRequest schema annotations | 只为已有required三维字段补同语义NotBlank；request构造/方法签名/旧读行为不变，不给override强化SPI参数 | 2 | REQ-001,REQ-002,REQ-012,REQ-013,REQ-014,REQ-018 | Step 2选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-contract/src/main/java/top/egon/cola/platform/tianquan/jianshen/contract/authorization/FieldPolicyDecision.java` | 当前存在；generic schema/cascade annotations | generic schema/cascade annotations | 原required字段NotNull/NotBlank、版本PositiveOrZero、Map键/FieldAccess cascade；同原ctor不新增权限规则/集合上限/JSON字段 | 2 | REQ-001,REQ-002,REQ-012,REQ-013,REQ-014,REQ-018 | Step 2选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/field/FieldWriteAuthorizationCommand.java` | 当前不存在；同模块已存在POM/邻近原包 | 三个key/submittedFields | class+WriteFields组、集合presence契约，不含caller tenant/subject | 2 | REQ-001,REQ-002,REQ-012,REQ-013,REQ-014,REQ-018 | Step 2选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/field/FieldWriteAuthorizationConverter.java` | 当前不存在；同模块已存在POM/邻近原包 | toTarget | BaseForwardConverter映射到真实三维FieldPolicyRequest | 2 | REQ-001,REQ-002,REQ-012,REQ-013,REQ-014,REQ-018 | Step 2选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/field/Rbac3FieldWriteAuthorizationComponent.java` | 当前不存在；同模块已存在POM/邻近原包 | requireWritableFields | schema→单次PDP→逐项WRITE，拒绝/不可用不执行业务 | 2 | REQ-001,REQ-002,REQ-012,REQ-013,REQ-014,REQ-018 | Step 2选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/authorization/Rbac3ObservedAuthorizationService.java` | 当前不存在；同模块已存在POM/邻近原包 | 原五个AuthorizationService方法 | Decorator保留delegate权限/异常，记录实际结果；Token上下文DENY沿既有usage MQ，0新HTTP | 9 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-011,REQ-012,REQ-014,REQ-015,REQ-018 | Step 9选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/autoconfigure/Rbac3StarterAutoConfiguration.java` | 当前存在；authorizationService/guard注册 | authorizationService/guard注册 | 原authorizationService Bean名保留，named delegate/observer/field converter | 2,9 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-011,REQ-012,REQ-013,REQ-014,REQ-015,REQ-018 | Step 2,9选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/web/Rbac3AuthorizationExceptionHandler.java` | 当前存在；denied/unavailable | denied/unavailable | 原两字段异常族，增加正确field guard异常映射/安全no-store；不重包正常响应 | 9 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-011,REQ-012,REQ-014,REQ-015,REQ-018 | Step 9选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/Rbac3AdminSecurityConfiguration.java` | 当前存在；两个原SecurityFilterChain | 两个原SecurityFilterChain | 明确entry point/denied handler、自身observer、原Idp→RBAC→tenant顺序 | 9 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-011,REQ-012,REQ-014,REQ-015,REQ-018 | Step 9选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityMetricsComponent.java` | 当前不存在；同模块已存在POM/邻近原包 | record | 同Core port→本host Micrometer，固定JIANSHEN角色 | 7 | REQ-001,REQ-009,REQ-010,REQ-011,REQ-014,REQ-015,REQ-018 | Step 7选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityObservationConfiguration.java` | 当前不存在；同模块已存在POM/邻近原包 | metrics/observer/readiness Beans | 同安全schema/依赖标准，绝不创建第二MeterRegistry | 7,10 | REQ-001,REQ-002,REQ-006,REQ-009,REQ-010,REQ-011,REQ-014,REQ-015,REQ-016,REQ-018,REQ-019 | Step 7,10选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/lombok.config` | 当前不存在；同模块已存在POM/邻近原包 | copyable Qualifier | 仅新/改业务Bean注入规范；不重构既有全域 | 2 | REQ-001,REQ-002,REQ-012,REQ-013,REQ-014,REQ-018 | Step 2选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml` | 当前存在；配置核心叶子键 | 配置核心叶子键 | §15同构；自身web启用，offline关闭 | 10 | REQ-001,REQ-002,REQ-006,REQ-014,REQ-015,REQ-016,REQ-018,REQ-019 | Step 10选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml` | 当前存在；配置核心叶子键 | 配置核心叶子键 | §15同构；自身web启用，offline关闭 | 10 | REQ-001,REQ-002,REQ-006,REQ-014,REQ-015,REQ-016,REQ-018,REQ-019 | Step 10选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-oauth-migration.yml` | 当前存在；配置核心叶子键 | 配置核心叶子键 | §15同构；自身web启用，offline关闭 | 10 | REQ-001,REQ-002,REQ-006,REQ-014,REQ-015,REQ-016,REQ-018,REQ-019 | Step 10选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/application.yml` | 当前存在；配置核心叶子键 | 配置核心叶子键 | §15同构；自身web启用，offline关闭 | 10 | REQ-001,REQ-002,REQ-006,REQ-014,REQ-015,REQ-016,REQ-018,REQ-019 | Step 10选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/application-local.yml` | 当前存在；配置核心叶子键 | 配置核心叶子键 | §15同构；自身web启用，offline关闭 | 10 | REQ-001,REQ-002,REQ-006,REQ-014,REQ-015,REQ-016,REQ-018,REQ-019 | Step 10选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/resources/application-local-it.yml` | 当前存在；测试profile核心键 | 测试profile核心键 | 继承/显式override parity，不隐启服务 | 10 | REQ-001,REQ-002,REQ-006,REQ-014,REQ-015,REQ-016,REQ-018,REQ-019 | Step 10选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/AuthArtifactContract.test.ts` | 当前不存在；同模块已存在POM/邻近原包 | 现有npm alias真实接口检查 | 保留声明/不制造新lock，只验证实际包无密码login/logout remoteState/stepUp | 11 | REQ-001,REQ-002,REQ-014,REQ-017,REQ-018,REQ-019 | Step 11选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/AuthArtifactContract.test.ts` | 当前不存在；同模块已存在POM/邻近原包 | 实际installed接口检查 | 校准旧产物后用真实包验证接口，非mock共享SDK | 11 | REQ-001,REQ-002,REQ-014,REQ-017,REQ-018,REQ-019 | Step 11选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/package-lock.json` | 当前存在；已声明共享包解析 | 已声明共享包解析 | 匹配PC-014 0.2.5制品；仅已批准publisher流程，不改认证源 | 11 | REQ-001,REQ-002,REQ-014,REQ-017,REQ-018,REQ-019 | Step 11选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthNativeCorsContractTest.java` | 当前不存在；同模块已存在POM/邻近原包 | OAuthNativeCorsContractTest | §14对应真实行为/范围回归，默认不启动外部设施 | 1 | REQ-001,REQ-002,REQ-003,REQ-014,REQ-016,REQ-018 | Step 1选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiContractTest.java` | 当前存在；OAuthProtocolOpenApiContractTest | OAuthProtocolOpenApiContractTest | §14对应真实行为/范围回归，默认不启动外部设施 | 1 | REQ-001,REQ-002,REQ-003,REQ-014,REQ-016,REQ-018 | Step 1选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpSecurityObservationFilterTest.java` | 当前不存在；同模块已存在POM/邻近原包 | IdpSecurityObservationFilterTest | §14对应真实行为/范围回归，默认不启动外部设施 | 6 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-015,REQ-016,REQ-018 | Step 6选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponentTest.java` | 当前存在；TokenUsageAuditComponentTest | TokenUsageAuditComponentTest | §14对应真实行为/范围回归，默认不启动外部设施 | 3,4 | REQ-001,REQ-002,REQ-006,REQ-007,REQ-008,REQ-014,REQ-015,REQ-018 | Step 3,4选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumerTest.java` | 当前存在；OAuthAuditConsumerTest | OAuthAuditConsumerTest | §14对应真实行为/范围回归，默认不启动外部设施 | 4 | REQ-001,REQ-002,REQ-006,REQ-007,REQ-008,REQ-014,REQ-015,REQ-018 | Step 4选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/TokenUsageAuditReceiptContractTest.java` | 当前不存在；同模块已存在POM/邻近原包 | TokenUsageAuditReceiptContractTest | §14对应真实行为/范围回归，默认不启动外部设施 | 4 | REQ-001,REQ-002,REQ-006,REQ-007,REQ-008,REQ-014,REQ-015,REQ-018 | Step 4选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/test/java/top/egon/cola/platform/tianquan/jianshen/starter/field/Rbac3FieldWriteAuthorizationComponentTest.java` | 当前不存在；同模块已存在POM/邻近原包 | Rbac3FieldWriteAuthorizationComponentTest | §14对应真实行为/范围回归，默认不启动外部设施 | 2 | REQ-001,REQ-002,REQ-012,REQ-013,REQ-014,REQ-018 | Step 2选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/Rbac3AdminSecurityChainTest.java` | 当前不存在；同模块已存在POM/邻近原包 | Rbac3AdminSecurityChainTest | §14对应真实行为/范围回归，默认不启动外部设施 | 9 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-011,REQ-012,REQ-014,REQ-015,REQ-018 | Step 9选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityMetricsContractTest.java` | 当前不存在；同模块已存在POM/邻近原包 | TianquanSecurityMetricsContractTest | §14对应真实行为/范围回归，默认不启动外部设施 | 7,8,10 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-010,REQ-011,REQ-014,REQ-015,REQ-016,REQ-018,REQ-019 | Step 7,8,10选择器/§8 |
| CREATE | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityMetricsContractTest.java` | 当前不存在；同模块已存在POM/邻近原包 | TianquanSecurityMetricsContractTest | §14对应真实行为/范围回归，默认不启动外部设施 | 7,9,10 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-010,REQ-011,REQ-012,REQ-014,REQ-015,REQ-016,REQ-018,REQ-019 | Step 7,9,10选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/MultiIssuerResourceServerTest.java` | 当前存在；MultiIssuerResourceServerTest | MultiIssuerResourceServerTest | §14对应真实行为/范围回归，默认不启动外部设施 | 5 | REQ-001,REQ-002,REQ-009,REQ-010,REQ-011,REQ-014,REQ-018 | Step 5选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2ClientTest.java` | 当前存在；IdpServiceOAuth2ClientTest | IdpServiceOAuth2ClientTest | §14对应真实行为/范围回归，默认不启动外部设施 | 5 | REQ-001,REQ-002,REQ-009,REQ-010,REQ-011,REQ-014,REQ-018 | Step 5选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/rpc/IdpRpcBearerServerInterceptor.java` | 当前存在；interceptCall / listener callbacks | interceptCall / listener callbacks | 传递Valid.proof到现有RPC Context | 6 | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-015,REQ-016,REQ-018 | Step 6选择器/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-contract/pom.xml` | 当前存在，缺少该编译能力 | 现有版本编译API/provided/processor按批准补齐 | 窄幅构建前置，无运行框架 | 2 | REQ-012,REQ-014 | Step2离线编译/真实Mapper/注入测试 |

| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/pom.xml` | 当前存在，缺少该编译能力 | 现有版本编译API/provided/processor按批准补齐 | 窄幅构建前置，无运行框架 | 2 | REQ-012,REQ-014 | Step2离线编译/真实Mapper/注入测试 |

| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/pom.xml` | 当前存在，缺少该编译能力 | 现有版本编译API/provided/processor按批准补齐 | 窄幅构建前置，无运行框架 | 7 | REQ-012,REQ-014 | Step7离线编译/真实Mapper/注入测试 |

| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfigurationTest.java` | 当前存在；按类型注册Mapper的旧fixture | ApplicationContextRunner bean name | 必要装配回归：使用Boot具名jacksonObjectMapper，不放宽断言 | 6 | REQ-001,REQ-002,REQ-014,REQ-015,REQ-018 | Step6 existing8 tests/具名装配 |

重复文件切片：IdpSecurityConfig=Step1 CORS/Step9 own错误；AuthorizationServerConfiguration=Step1 OPTIONS/Step8 UserInfo原decoder；TokenUsageAuditComponent=Step3 strict/Step4确认；IdpJwtVerifier=Step5计时/Step6捕获与managed观察；两个auto config按各Step仅接入已完成能力；两个ObservationConfiguration=Step7 host指标/Step10部署验证；各测试按对应行为增补。所有文件只在最终已完成行为的commit中提交，后续编辑不得移除原Step验证。

| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthTokenClientCredentialsTest.java` | 当前存在；old standalone security fixture | SecurityConfiguration import | Step1 corrective fixture：只引入既有Boot ValidationAutoConfiguration供defaultValidator | Step1 corrective | REQ-002,REQ-014,REQ-018 | corrective focused11/原SAS客户端模式4tests |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/IdpPlatformConfiguration.java` | 当前存在；PC-011必要readiness消费 | idpRuntimeReadiness | 组合 qualified audit BooleanSupplier，不覆盖原guard | 10 | REQ-014,REQ-015,REQ-016,REQ-019 | Step10 focused/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/runtime/Rbac3PlatformIntegrationConfiguration.java` | 当前存在；PC-011必要readiness消费 | rbac3ReadinessIndicator | 组合 qualified audit BooleanSupplier，不覆盖原guard | 10 | REQ-014,REQ-015,REQ-016,REQ-019 | Step10 focused/§8 |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/package.json` | 当前存在；PC-012解析修正 | dependency logical key | 复用JW npm alias，不改源码import/range | 11 | REQ-017,REQ-019 | 实际npmjs artifact与两host typecheck/test |
| MODIFY | `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/package.json` | 原文件存在；用户确认实际SDK名称 | 既有SDK dependency | npm alias到同版本rbac3-react-sdk，原import保持 | 11 | REQ-017,REQ-019 | 实际安装/tsc/Vitest |
| MODIFY | `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/package.json` | 原文件存在；用户明确授权0.2.5发布 | publisher元数据/文档 | 仅release身份与root version/文档，不改认证源码或他平台 | 11 | REQ-017,REQ-019 | pack/build/源suite/两个host/registry |
| MODIFY | `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/package-lock.json` | 原文件存在；用户明确授权0.2.5发布 | publisher元数据/文档 | 仅release身份与root version/文档，不改认证源码或他平台 | 11 | REQ-017,REQ-019 | pack/build/源suite/两个host/registry |
| MODIFY | `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/README.md` | 原文件存在；用户明确授权0.2.5发布 | publisher元数据/文档 | 仅release身份与root version/文档，不改认证源码或他平台 | 11 | REQ-017,REQ-019 | pack/build/源suite/两个host/registry |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

工作目录`/Users/mario/SelfProject/Egon-COLA`；分支main，HEAD `3a82ea27652232c741b3366e94da8b3b36ea03bd`。开始时只有未跟踪主Spec；生成后只有主Spec和本Plan文档变化，无staged/source/test改动。用户给定AGENTS要求最小scope、不改历史Flyway、不自动启项目、不用GUI/browser、每逻辑任务至多一个commit；仓库搜索没有额外AGENTS.md。保留ignored/untracked用户资料（包括scripts/work已有冻结文件），不清理、不git add -A。每Step执行前重新git status/HEAD，若独立改动撞到本Step路径先明确归属，不自动覆盖。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Java/Maven | 根POM Java21/Boot3.5.16；xingyuan POM Surefire3.2.5；mvn -o | 本地JDK21与既有Maven artifacts/processor，缺缓存不自动下载 | 目标选择器test，禁止无guard完整reactor |
| 新/改测试 | §7每Step完整mvn -pl -am -Dtest列表 | 新测试不得Testcontainers/真实PG/Redis自动连接；report内实际有命名测试 | -am编译依赖不代表运行全部上游测试；failIfNoSpecifiedTests=false不得掩盖目标0 tests |
| Frontend | SW/JW package.json及publisher既有scripts | PC-014授权构建发布源码一致0.2.5，再registry安装；npm/Vitest/tsc现有工具链 | 仅八声明文件，不改认证源；先验包后精确发布、可信回取；其他host不接入 |
| Runtime | 主目标§16 SELF-RUNTIME | 需用户另授权并提供PG/Redis PRIMARY/卷/证书/ACL/key/实际入口 | 本Plan/当前turn均不执行部署、DB、browser、负载 |
| 文档 | 两个writing skills的resource预检与strict validator | 已有Python3；主Spec Accepted/相对links有效 | 仅结构/覆盖/文件scope，非安全runtime证明 |

### 6.3 Immutable constraints and approved decisions

原11字段usage schemaVersion1、Streams/group、原spool frame可读、180天retention、旧USER/SERVICE typed ports与factory、manager 2/3参构造、Valid 1/2参、RPC旧with/run、FieldPolicyRequest三key、authorizationService Bean名与替代语义、OAuth/RBAC/Controller三族成功/错误、tenant finally、密码/PKCE/RS256/撤销世代、SQL/manifest/唯一键/MP同源事务均保持。API_OPTIONS只技术Query，原mapped OpenAPI annotations不变。新receipt六字段schema1、domain-separated HMAC/规范11字段指纹、DEAD不清帧是已批准要求，不留自由选择。新Spring Bean依赖Lombok具名qualified注入；仅旧public兼容技术构造/factory保留适配，不手写新的业务DI构造。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| PLAN-CLAR-001 | 新测试方法名按File blocks；签名编译前置在同Step内两次编辑后一起GREEN提交 | 新路径当前不存在、旧契约可直接委托；skill允许先contract再behavior RED | 没有独立stub commit或对外启用，算法/输出均由Spec固定 | 若不能在相同Step产生真实RED，暂停该Step，不把编译失败算通过 |
| PLAN-CLAR-002 | 使用Common ValidationUtils实例API与具名defaultValidator；技术factory复用已有Validator提供该facade | Common构造ValidationUtils(Validator)/实例validate；ResourceServerServiceImpl既有@Qualifier("defaultValidator") | 不新增校验框架/Utils类；仍按原schema groups | 发现Bean缺失必须修本平台装配/测试，不跳过边界 |
| PLAN-CLAR-003 | managed观察组合放IdpJwtVerifier配置factory，保持两个final typed wrapper原文件/类型 | UserAccessTokenVerifier/ServiceAccessTokenVerifier都是final，verify直接委托IdpJwtVerifier | 已批准的原验证Function/Decorator落点，public ports不变；不能继承final包装器 | 若当前factory无法保存外层唯一观察语义，回Spec，不另开端口/改其他platform |
| PLAN-CLAR-004 | JWK cache固定使用startup绑定recorder；manager旧2/3参构造委托noop兼容 | BoundedJwkSetCache.get无请求参数；Edge旧构造实际仍调用 | 观测不改变cache key/TTL/流控，不新增request ThreadLocal | 若必须改Edge源码才可编译则阻塞，不扩大范围 |
编码细节：receipt指纹/签名共同复用TokenUsageAuditComponent技术codec；UTF-8字段以DataOutputStream.writeInt的big-endian长度前缀编码，null为-1，原11字段按Spec顺序，Instant.toString与issuer.toASCIIString；不是任意JSON hash。PLAN-CLAR-005：本地Security6.5.11 CorsConfigurer.configure直接addFilter而不postProcess；因此Step1在同一声明安全链显式创建原CorsFilter、setCorsProcessor、http.addFilter，保持既有filter顺序/策略/路径与7文件范围。该修正复用本文件已有显式filter装配惯例，非行为或架构变更。RPC interceptor不是Plan推断：用户已明确批准补齐Spec §8表，已同步；旧字面“只写Spec”属于上一轮阶段，当前用户明确授权写Plan。其他未提供运行输入不被伪造为技术决策。

## 7. Ordered File-by-file Implementation Steps

本章所有路径均为未来变更。逐Step串行；表中前置签名是为了使行为RED可运行，必须与完整GREEN实现处于同一个Step/commit。新record/class/DTO与配置仅建立编译/schema前置，不代表使用入口已接通。

### Step 1 — 限定原生 OAuth CORS 并同步生成合同

- Requirements: REQ-001, REQ-002, REQ-003, REQ-014, REQ-016, REQ-018
- Dependencies: None；基线HEAD与原目标tests需先记录
- Baseline state: 当前基线缺口：现有真实安全链不为四tenant资源产生正确允许头；缺header的OPTIONS可能进入错误链；OAS没有对应四OPTIONS
- Observable outcome: 限定原生 OAuth CORS 并同步生成合同；以本Step命名真实行为断言证明，错误分支和负作用同时核对。
- End state: 四资源协商真实链/OAS一致；Cookie保护与Admin同源保持；审计仍原开关。
- Test-first gate: Required — 现有真实安全链不为四tenant资源产生正确允许头；缺header的OPTIONS可能进入错误链；OAS没有对应四OPTIONS；按File顺序签名编译前置后才运行RED，编译/fixture/环境失败不计。
- Manual Checks: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001；按每File Standards impact及本Step验证分别闭合。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthNativeCorsContractTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: OAuthNativeCorsContractTest；native_preflight_restricted_contract。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，原IdpSecurityConfig/CorsFilter/实际SAS链与MockMvc是现成邻近能力。
- Dependencies and consumers: 原IdpSecurityConfig/CorsFilter/实际SAS链与MockMvc；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 新增/扩充native_preflight_restricted_contract方法；TEST-001–003受控行为断言，默认不启PG/Redis/Testcontainers。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: 四资源合法origin/真实method返回200无body；wild/dup/缺header/溢出tenant403无Allow；login/consent原CSRF403，Admin无CORS
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void native_preflight_restricted_contract() {
    arrange 复用原IdpSecurityConfig/CorsFilter/实际SAS链与MockMvc，不启动应用或外部基础设施;
    act 调用该Step实际生产入口，不以手工调用record()/mock整段授权替代真实分支;
    assert 四资源合法origin/真实method返回200无body；wild/dup/缺header/溢出tenant403无Allow；login/consent原CSRF403，Admin无CORS;
    verify 所有拒绝/不可用分支businessSpy为零且敏感值不进入event/labels/JSON;
}
```

- Verification contribution: 本Step `OAuthNativeCorsContractTest,OAuthProtocolOpenApiContractTest,IdpPublishedOpenApiContractTest`；四资源合法origin/真实method返回200无body；wild/dup/缺header/溢出tenant403无Allow；login/consent原CSRF403，Admin无CORS 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiContractTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: OAuthProtocolOpenApiContractTest；native_preflight_openapi_contract。
- Repository evidence: 当前路径存在，已有OAuthProtocolOpenApiContractTest；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 既有OAuthProtocolOpenApiContractTest/原Customizer/Jackson；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 新增/扩充native_preflight_openapi_contract方法；TEST-004受控行为断言，默认不启PG/Redis/Testcontainers。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: 生成OAS四唯一OPTIONS operationId、security=[]、200/403/headers、无body/content；原GET/POST schema保持
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void native_preflight_openapi_contract() {
    arrange 复用既有OAuthProtocolOpenApiContractTest/原Customizer/Jackson，不启动应用或外部基础设施;
    act 调用该Step实际生产入口，不以手工调用record()/mock整段授权替代真实分支;
    assert 生成OAS四唯一OPTIONS operationId、security=[]、200/403/headers、无body/content；原GET/POST schema保持;
    verify 所有拒绝/不可用分支businessSpy为零且敏感值不进入event/labels/JSON;
}
```

- Verification contribution: 本Step `OAuthNativeCorsContractTest,OAuthProtocolOpenApiContractTest,IdpPublishedOpenApiContractTest`；生成OAS四唯一OPTIONS operationId、security=[]、200/403/headers、无body/content；原GET/POST schema保持 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthCorsProperties.java`

- Purpose: 校验已有白名单key，不创造自动反射/通配符；本次只完成本Step所属切片。
- Symbols: allowedOrigins；OAuthCorsProperties。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，原allowed-origins key/Boot binder；IdpSecurityConfig/OAuthNativeCorsProcessor是现成邻近能力。
- Dependencies and consumers: 原allowed-origins key/Boot binder；IdpSecurityConfig/OAuthNativeCorsProcessor；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: @ConfigurationProperties("egon.tianquan-shoubing.oauth") class OAuthCorsProperties复用allowed-origins。
- Input/output and state mapping: 0..64精确origins、各≤2048完整origin，startup canonical URI校验；无通配/路径/查询等。
- Error and edge behavior: 空集合全部不开放；local loopback HTTP只在Spec多重开关成立；不得用Host反射；不改其他OAuth properties。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001：普通class必须@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder；根class不callSuper；字段annotations/default/groups见上述contract。 无业务DI；Date禁用，JSON采用Boot Jackson。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：上述精确角色、约束/建模与UTC Duration/Instant；URI及hash不可变；不另建模型/新层。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder @Validated
@ConfigurationProperties("egon.tianquan-shoubing.oauth")
class OAuthCorsProperties { @Size(max=64) @NotNull @Builder.Default List<@NotBlank @Size(max=2048) String> allowedOrigins=List.of(); }
// startup逐个URI(origin)：scheme/host/port，无userinfo/path/query/fragment/wildcard；collections防御复制。
```

- Verification contribution: 本Step `OAuthNativeCorsContractTest,OAuthProtocolOpenApiContractTest,IdpPublishedOpenApiContractTest`；空集合全部不开放；local loopback HTTP只在Spec多重开关成立；不得用Host反射；不改其他OAuth properties。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthNativeCorsProcessor.java`

- Purpose: Decorator复用DefaultCorsProcessor，仅限制四资源preflight单值header/方法；本次只完成本Step所属切片。
- Symbols: processRequest；OAuthNativeCorsProcessor。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，现有Spring CorsProcessor/CorsConfigurationSource；具名oauthNativeCorsProcessor factory/原chain是现成邻近能力。
- Dependencies and consumers: 现有Spring CorsProcessor/CorsConfigurationSource；具名oauthNativeCorsProcessor factory/原chain；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: Decorator DefaultCorsProcessor的processRequest(CorsConfiguration,HttpServletRequest,HttpServletResponse)。
- Input/output and state mapping: canonical正Long tenant path+四资源识别→Origin/ACR method/header单值限制→Spring原processor；不查tenant存在。
- Error and edge behavior: token POST其余GET；headers≤16、每项≤64、Authorization/Content-Type子集、重复拒绝；403 text/plain Invalid CORS request，无allow头；成功bodyless200/Vary3/600s。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
processRequest(config,request,response) {
    if (四资源OPTIONS) { 校验canonical tenant与Origin/ACR单值及精确method/header; 失败用DefaultCorsProcessor.rejectRequest语义403且无Allow; }
    return defaultCorsProcessor.processRequest(config,request,response);
}
// non-preflight同源无Origin保留；禁登录/consent/authorize/platform/admin跨域；不创建业务Controller。
```

- Verification contribution: 本Step `OAuthNativeCorsContractTest,OAuthProtocolOpenApiContractTest,IdpPublishedOpenApiContractTest`；token POST其余GET；headers≤16、每项≤64、Authorization/Content-Type子集、重复拒绝；403 text/plain Invalid CORS request，无allow头；成功bodyless200/Vary3/600s。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/IdpSecurityConfig.java`

- Purpose: 保留自身Bearer链，复用错误响应；CORS不开放Admin；本次只完成本Step所属切片。
- Symbols: idpCorsConfigurationSource / 原链CORS factory；IdpSecurityConfig。
- Repository evidence: 当前路径存在，已有CORS/自有观察filter/错误族；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: OAuthCorsProperties/OAuthNativeCorsProcessor/原CorsConfigurationSource；原SAS及Admin链；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 本Step仅注册OAuthCorsProperties/oauthNativeCorsProcessor并收敛corsConfigurationSource；不引用Step6观察filter。
- Input/output and state mapping: 原allowedOrigins→四route POST或GET、credentials=false；原Idp→RBAC链保持，新增observer仅自身。
- Error and edge behavior: health/preflight不产生usage；Admin同源不开放CORS；401/403/503必须filter层处理，OAuth原错误族不换wrapper。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
@Bean(name="oauthCorsProperties") 绑定已校验的原allowed-origins，集合为空默认不开放;
@Bean(name="oauthNativeCorsProcessor") 用具名DefaultCorsProcessor依赖创建Lombok Decorator;
idpCorsConfigurationSource(properties) { 仅注册tenant token POST、userinfo/discovery/JWKS GET；headers Authorization/Content-Type，credentials=false，maxAge600; }
// 本Step不改认证错误/观察装配，不引用后续新类型；严格firewall不放宽，原生4路径OPTIONS的rejected-request渲染映射403，其余原样。
```

- Verification contribution: 本Step `OAuthNativeCorsContractTest,OAuthProtocolOpenApiContractTest,IdpPublishedOpenApiContractTest`；health/preflight不产生usage；Admin同源不开放CORS；401/403/503必须filter层处理，OAuth原错误族不换wrapper。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java`

- Purpose: 将四技术preflight纳入同原生链，Cookie CSRF及协议Provider保持；本次只完成本Step所属切片。
- Symbols: authorizationServerSecurityFilterChain matcher / CorsFilter processor；AuthorizationServerConfiguration。
- Repository evidence: 当前路径存在，已有protocol matcher/cors object processor；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: Step1具名OAuthNativeCorsProcessor/CorsConfigurationSource；原SAS endpointsMatcher；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 本Step只增加四资源OPTIONS matcher和已配置processor的原CorsFilter，UserInfo metrics在Step8。
- Input/output and state mapping: 原SAS configure保持multipleIssuers/PKCE/CSRF；四OPTIONS无凭据协商；UserInfo真实RSA解码→timer/verified capture同步回调。
- Error and edge behavior: 无Origin OPTIONS也进入限定processor403；实际同源无Origin不拒绝；密码学时间不含DB/Redis/usage；不新增second JwtDecoder。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
nativeOptionsMatcher=OPTIONS AND /tenants/*/四个原生资源后缀；其中tenant格式交processor校验以保证非canonical值403;
http.securityMatcher(原SAS endpointsMatcher OR nativeOptionsMatcher);
nativeCors = new CorsFilter(原source); nativeCors.setCorsProcessor(oauthNativeCorsProcessor); http.addFilter(nativeCors);
// 登录/consent CSRF原样；OPTIONS缺Origin也由限定processor403；此时不改UserInfo decoder/producer或引用未来观察类。
```

- Verification contribution: 本Step `OAuthNativeCorsContractTest,OAuthProtocolOpenApiContractTest,IdpPublishedOpenApiContractTest`；无Origin OPTIONS也进入限定processor403；实际同源无Origin不拒绝；密码学时间不含DB/Redis/usage；不新增second JwtDecoder。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 7 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiConfiguration.java`

- Purpose: 现有Customizer记录200/403/no body；原协议schema保留；本次只完成本Step所属切片。
- Symbols: 四options operations；OAuthProtocolOpenApiConfiguration。
- Repository evidence: 当前路径存在，已有四options operations；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 现有springdoc MVC OAS3 models；OAuthProtocolOpenApiContractTest / published OAS；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: existing OpenApiCustomizer仅增加四options；operationIds按API-001..004固定。
- Input/output and state mapping: token/UserInfo/discovery/JWKS原PathItem上补options、tenant/Origin/ACR参数、200/403 headers、security=[]、无requestBody/content。
- Error and edge behavior: 原GET/POST schemas/status/operationId保持；不添加伪Controller/@Operation target，不暴露管理docs。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
原oauthProtocolOpenApiCustomizer(openApi) {
    四path按Spec §9.1各添加new Operation().operationId(固定PreflightID).security(List.of());
    设置Origin/ACR与tenantId参数、200/no content/Allow/Vary/MaxAge、403/text无业务JSON;
    assert原GET/POST对象未被覆盖;
}
```

- Verification contribution: 本Step `OAuthNativeCorsContractTest,OAuthProtocolOpenApiContractTest,IdpPublishedOpenApiContractTest`；原GET/POST schemas/status/operationId保持；不添加伪Controller/@Operation target，不暴露管理docs。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`（Maven）；Step11 npm用下列完整 --prefix，无服务进程。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthNativeCorsContractTest,OAuthProtocolOpenApiContractTest,IdpPublishedOpenApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；该Step `git diff --check` 与精确commit path审查。
- Expected result: RED必须是上述缺失行为断言；GREEN exit0、目标命名tests实际执行且0 failures/errors/非预期skip；无源schema、Bean注入/敏感值或旧机制退化。Step11普通解析不得走临时alias。
- Failure returns to: 本Step对应assert的生产File；schema/DI到对应载体/factory，receipt状态到Step3/4，原合同退化停止当前Step；artifact缺失停止Step11等待可信输入；若需加库/改SQL/外platform或改Spec语义，回用户/Spec。
- Completion criteria: 四资源协商真实链/OAS一致；Cookie保护与Admin同源保持；审计仍原开关；保存实际RED/GREEN命令、测试report/准确数量、适用MC证据及当前HEAD，没有业务未完成签名/placeholder或外部runtime假成功。
- Rollback: 未部署时只还原本Step相对前置HEAD的精确diff，保留之前Step及未归属改动；已产生审计/spool时禁止删帧/回旧consumer，按§9停止入口并forward-fix。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthNativeCorsContractTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiContractTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthCorsProperties.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthNativeCorsProcessor.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/IdpSecurityConfig.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiConfiguration.java`.
- Commit: `fix(shoubing): restrict native OAuth CORS`；本轮只规划，不执行。


### Step 2 — 提供显式字段写权限闸门

- Requirements: REQ-001, REQ-002, REQ-012, REQ-013, REQ-014, REQ-018
- Dependencies: None；基线HEAD与原目标tests需先记录
- Baseline state: 当前基线缺口：只委托旧PDP的编译前置闸门会把overall ALLOW下的READ或缺字段当可写；断言拒绝和businessSpy零调用失败
- Observable outcome: 提供显式字段写权限闸门；以本Step命名真实行为断言证明，错误分支和负作用同时核对。
- End state: 平台guard单次PDP+全WRITE成立，三维旧请求/读serializer兼容；没有业务接口自动接入。
- Test-first gate: Required — 只委托旧PDP的编译前置闸门会把overall ALLOW下的READ或缺字段当可写；断言拒绝和businessSpy零调用失败；按File顺序签名编译前置后才运行RED，编译/fixture/环境失败不计。
- Manual Checks: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001；按每File Standards impact及本Step验证分别闭合。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/pom.xml`

- Purpose: 已批准的编译能力前置，在行为测试前解除工具链缺口，不单独作为业务GREEN提交。
- Symbols: Lombok provided/optional 1.18.46；processor MapStruct1.6.3/binding0.2.0；原Maven compiler配置。
- Repository evidence: 本轮Step2 preflight表给出当前POM/父依赖和缓存证据，用户明确批准。
- Dependencies and consumers: 当前Root Boot/BOM/Lombok与授柄既有MapStruct模式；本模块后续模型/组件编译消费者。
- Why now: 无此项，后续RED只能得到编译失败，不能验证批准的业务行为。
- Contract/signature changes: 仅上述编译API/provided/processor，不修改public Java/HTTP或SQL。
- Input/output and state mapping: POM现有版本→离线compile/generated目标；无DB/配置状态变化。
- Error and edge behavior: 缓存缺失停止，不下载；版本与已批准不符阻塞；原依赖保留。
- Standards impact: MC-DEP-001, MC-REUSE-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001：已有库/规范构造与mapper的最小支持。
- Literal rule enforcement: Rule 2, Rule 3, Rule 4, Rule 11：注解校验、完整Lombok/真实MapStruct生成，不以手写绕过。
- Implementation pseudocode:

```xml
<dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><version>${lombok.version}</version><scope>provided</scope><optional>true</optional></dependency>
<!-- Lombok provided/optional 1.18.46；processor MapStruct1.6.3/binding0.2.0；继承Boot配置处理器与Lombok，JS追加现版本MapStruct/binding -->
<!-- 无新runtime依赖/升级/下载，真实generated Mapper或构造注入后验证 -->
```

- Verification contribution: 本Step原命名测试及离线编译；JS必须存在FieldWriteAuthorizationConverterImpl，JA验证生成Qualified构造。
- After this file: 编译前置可用，原行为未扩；继续本Step其余严格File顺序，RED/GREEN后一起提交。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-contract/pom.xml`

- Purpose: 已批准的编译能力前置，在行为测试前解除工具链缺口，不单独作为业务GREEN提交。
- Symbols: Jakarta Validation API 3.0.2；原Maven compiler配置。
- Repository evidence: 本轮Step2 preflight表给出当前POM/父依赖和缓存证据，用户明确批准。
- Dependencies and consumers: 当前Root Boot/BOM/Lombok与授柄既有MapStruct模式；本模块后续模型/组件编译消费者。
- Why now: 无此项，后续RED只能得到编译失败，不能验证批准的业务行为。
- Contract/signature changes: 仅上述编译API/provided/processor，不修改public Java/HTTP或SQL。
- Input/output and state mapping: POM现有版本→离线compile/generated目标；无DB/配置状态变化。
- Error and edge behavior: 缓存缺失停止，不下载；版本与已批准不符阻塞；原依赖保留。
- Standards impact: MC-DEP-001, MC-REUSE-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001：已有库/规范构造与mapper的最小支持。
- Literal rule enforcement: Rule 2, Rule 3, Rule 4, Rule 11：注解校验、完整Lombok/真实MapStruct生成，不以手写绕过。
- Implementation pseudocode:

```xml
<dependency><groupId>jakarta.validation</groupId><artifactId>jakarta.validation-api</artifactId></dependency>
<!-- Boot3.5.16管理3.0.2，不引入运行starter；后续模型metadata才能编译 -->
<!-- 原Jackson/测试依赖和契约JSON不变 -->
```

- Verification contribution: 本Step原命名测试及离线编译；JS必须存在FieldWriteAuthorizationConverterImpl，JA验证生成Qualified构造。
- After this file: 编译前置可用，原行为未扩；继续本Step其余严格File顺序，RED/GREEN后一起提交。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/authorization/AuthorizationService.java`

- Purpose: 只为已有required三维字段补同语义NotBlank；request构造/方法签名/旧读行为不变，不给override强化SPI参数；本次只完成本Step所属切片。
- Symbols: nested FieldPolicyRequest schema annotations；AuthorizationService。
- Repository evidence: 当前路径存在，已有nested FieldPolicyRequest schema annotations；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 原DefaultAuthorizationService/FieldPolicyRequest使用者；新generated field converter/guard；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 仅nested FieldPolicyRequest(String permissionCode,String applicationCode,String resourceCode)增加generic @NotBlank。
- Input/output and state mapping: 输入三维required约束→原compact constructor trim/invariants，原decideFields签名不变。
- Error and edge behavior: 不加submittedFields/新的长度上限/权限规则；不在override加@Valid/@NotNull导致HV000151；旧读路径代理/异常不变。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-ARCH-001：接口仅契约/原nested record metadata，无具体业务Bean注入或日志；default约束不强化override参数，纯Core不反向依赖host。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11：精确角色/原三维record或单operation接口，注解语义等同原constructor；不扩public参数。
- Implementation pseudocode:

```java
record FieldPolicyRequest(@NotBlank String permissionCode,@NotBlank String applicationCode,@NotBlank String resourceCode) {
    // 保留原required/trim compact ctor，不改变三维key语义
}
FieldPolicyDecision decideFields(FieldPolicyRequest request); // SPI参数约束原样
// 新guard显式用Default group检查该值对象；原读serializer调用方式不改。
```

- Verification contribution: 本Step `Rbac3FieldWriteAuthorizationComponentTest,DefaultAuthorizationServiceTest,Rbac3FieldPropertyWriterTest,Rbac3MethodAuthorizationManagerTest`；不加submittedFields/新的长度上限/权限规则；不在override加@Valid/@NotNull导致HV000151；旧读路径代理/异常不变。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-contract/src/main/java/top/egon/cola/platform/tianquan/jianshen/contract/authorization/FieldPolicyDecision.java`

- Purpose: 原required字段NotNull/NotBlank、版本PositiveOrZero、Map键/FieldAccess cascade；同原ctor不新增权限规则/集合上限/JSON字段；本次只完成本Step所属切片。
- Symbols: generic schema/cascade annotations；FieldPolicyDecision。
- Repository evidence: 当前路径存在，已有generic schema/cascade annotations；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 原contract消费者/Starter；field guard default/cascade；原JSON serializer；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 原FieldPolicyDecision/FieldAccess泛型Jakarta schema/cascade注解；无新字段/JSON形状。
- Input/output and state mapping: decision/time required；reason/tenant/subject/三个key NotBlank；versions PositiveOrZero；Map键NotBlank/值NotNull Valid，evidenceIds元素NotBlank。
- Error and edge behavior: 全部与原compact ctor相等语义；不新collection长度/权限规则；immutable Map/List防御复制保留；maskingStrategy可null，非null依原非blank约束。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001：仅真正immutable record保留，集合防御复制/JsonIgnore proof；原构造和wire不变。 无业务DI；Date禁用，JSON采用Boot Jackson。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：上述精确角色、约束/建模与UTC Duration/Instant；URI及hash不可变；不另建模型/新层。
- Implementation pseudocode:

```java
record FieldPolicyDecision(@NotNull Decision decision,@NotBlank String reasonCode,
    @NotBlank String tenantId,@NotBlank String subjectId,@NotBlank String permissionCode,
    @NotBlank String applicationCode,@NotBlank String resourceCode,
    @NotNull Map<@NotBlank String,@NotNull @Valid FieldAccess> fields,
    @PositiveOrZero long authVersion,@PositiveOrZero long policyVersion,
    @NotNull List<@NotBlank String> evidenceIds,@NotNull Instant decidedAt) { 原compact ctor/防御复制保留; }
record FieldAccess(@NotNull FieldAccessLevel level,String maskingStrategy) { 原optional非null非blank规则保留; }
```

- Verification contribution: 本Step `Rbac3FieldWriteAuthorizationComponentTest,DefaultAuthorizationServiceTest,Rbac3FieldPropertyWriterTest,Rbac3MethodAuthorizationManagerTest`；全部与原compact ctor相等语义；不新collection长度/权限规则；immutable Map/List防御复制保留；maskingStrategy可null，非null依原非blank约束。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/field/FieldWriteAuthorizationCommand.java`

- Purpose: class+WriteFields组、集合presence契约，不含caller tenant/subject；本次只完成本Step所属切片。
- Symbols: 三个key/submittedFields；FieldWriteAuthorizationCommand。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，服务端显式调用者/FieldWriteAuthorizationConverter/Component；本轮仅自测是现成邻近能力。
- Dependencies and consumers: 服务端显式调用者/FieldWriteAuthorizationConverter/Component；本轮仅自测；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: class FieldWriteAuthorizationCommand：三个code+Set<String> submittedFields，内部WriteFields group。
- Input/output and state mapping: code 1..128 ASCII [A-Za-z0-9._:-]；set1..100同code；服务器presence包含显式null，不含tenant/subject/role。
- Error and edge behavior: 无trim/case；无输入自动过滤；builder/setter/getter defensive copy；null/empty/oversize schema失败在PDP之前。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001：普通class必须@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder；根class不callSuper；字段annotations/default/groups见上述contract。 无业务DI；Date禁用，JSON采用Boot Jackson。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：上述精确角色、约束/建模与UTC Duration/Instant；URI及hash不可变；不另建模型/新层。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder
class FieldWriteAuthorizationCommand { interface WriteFields {}
    @NotBlank(groups=WriteFields.class) @Size(max=128,groups=WriteFields.class) @Pattern(regexp="[A-Za-z0-9._:-]+",groups=WriteFields.class) String permissionCode; // applicationCode/resourceCode同约束
    @NotEmpty(groups=WriteFields.class) @Size(max=100,groups=WriteFields.class) Set<@NotBlank(groups=WriteFields.class) @Size(max=128,groups=WriteFields.class) @Pattern(regexp="[A-Za-z0-9._:-]+",groups=WriteFields.class) String> submittedFields;
    // getter/setter与builder完成集合复制；不从DTO非null值自动计算集合。
}
```

- Verification contribution: 本Step `Rbac3FieldWriteAuthorizationComponentTest,DefaultAuthorizationServiceTest,Rbac3FieldPropertyWriterTest,Rbac3MethodAuthorizationManagerTest`；无trim/case；无输入自动过滤；builder/setter/getter defensive copy；null/empty/oversize schema失败在PDP之前。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/field/FieldWriteAuthorizationConverter.java`

- Purpose: BaseForwardConverter映射到真实三维FieldPolicyRequest；本次只完成本Step所属切片。
- Symbols: toTarget；FieldWriteAuthorizationConverter。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，Common BaseForwardConverter/MapStruct既有processor；field guard是现成邻近能力。
- Dependencies and consumers: Common BaseForwardConverter/MapStruct既有processor；field guard；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: @Mapper(componentModel="spring") BaseForwardConverter<FieldWriteAuthorizationCommand,AuthorizationService.FieldPolicyRequest>。
- Input/output and state mapping: 只三个key→原record constructor；submittedFields不进入PDP请求，集合保留在guard。
- Error and edge behavior: ignoreByDefault显式映射三个字段；null不作默认key；转换后Default校验；generated bean fieldWriteAuthorizationConverterImpl。
- Standards impact: MC-CONVERT-001, MC-BEAN-001, MC-VALID-001, MC-MODEL-001：@Mapper(componentModel="spring")+Common BaseForwardConverter、ignoreByDefault映射；generated实现具名，真实生成代码测试，接口无业务Slf4j/构造注入。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 11：generated mapper及字段明确映射、目标schema手交接验证；不可手工copy/JSON往返替代。
- Implementation pseudocode:

```java
@Mapper(componentModel="spring") interface FieldWriteAuthorizationConverter extends BaseForwardConverter<FieldWriteAuthorizationCommand,AuthorizationService.FieldPolicyRequest> {
    @BeanMapping(ignoreByDefault=true)
    @Mapping(target="permissionCode",source="permissionCode") @Mapping(target="applicationCode",source="applicationCode") @Mapping(target="resourceCode",source="resourceCode")
    AuthorizationService.FieldPolicyRequest toTarget(FieldWriteAuthorizationCommand source);
}
// 无手写set/get、BeanUtils或JSON拷贝；submittedFields是源字段，目标无同名字段。
```

- Verification contribution: 本Step `Rbac3FieldWriteAuthorizationComponentTest,DefaultAuthorizationServiceTest,Rbac3FieldPropertyWriterTest,Rbac3MethodAuthorizationManagerTest`；ignoreByDefault显式映射三个字段；null不作默认key；转换后Default校验；generated bean fieldWriteAuthorizationConverterImpl。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/field/Rbac3FieldWriteAuthorizationComponent.java`

- Purpose: 同Step编译前置：schema→单次PDP→逐项WRITE，拒绝/不可用不执行业务；仅建立兼容签名/原委托，暂不注册/启用，后续同路径完成GREEN。
- Symbols: requireWritableFields；Rbac3FieldWriteAuthorizationComponent。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，qualified authorizationService/currentRbac3User/fieldWriteAuthorizationConverterImpl/defaultValidator；原field read与method PEP均不修改是现成邻近能力。
- Dependencies and consumers: qualified authorizationService/currentRbac3User/fieldWriteAuthorizationConverterImpl/defaultValidator；原field read与method PEP均不修改；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 当前测试所需契约尚不存在，先建立仅原委托的可编译入口；后续立即真实RED。
- Contract/signature changes: FieldPolicyDecision requireWritableFields(@Valid FieldWriteAuthorizationCommand command)，@Validated(WriteFields.class)。
- Input/output and state mapping: WriteFields schema→currentRbac3User.require().identity→generated request/default校验→一次authorizationService.decideFields→decision default/cascade→overall ALLOW且每项WRITE→原decision。
- Error and edge behavior: 无USER/SERVICE用现有AuthenticationCredentialsNotFoundException拒绝，不伪造IdentityPrincipal；DENY/任一缺字段或非WRITE→org.springframework.security.authorization.AuthorizationDeniedException("FIELD_ACCESS_DENIED")；INDETERMINATE→RuntimeUnavailableException(reason,identity)；所有失败0业务写。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
// 同Step签名编译前置；不代表GREEN，暂不启用生产Bean或独立提交。
FieldPolicyDecision requireWritableFields(@Valid FieldWriteAuthorizationCommand command)，@Validated(WriteFields.class)。
// observe/requireWritableFields/appendStrict仅委托原验证/PDP/append；metric record暂不发信号，配置只声明具名factory。
// 后续测试必须因缺失字段WRITE/strict durability/usage/metrics行为失败，再回本文件按完整pseudocode实现。
```

- Verification contribution: 本Step `Rbac3FieldWriteAuthorizationComponentTest,DefaultAuthorizationServiceTest,Rbac3FieldPropertyWriterTest,Rbac3MethodAuthorizationManagerTest`；只满足编译，不能代替下一RED验证。
- After this file: 签名可编译但行为尚缺，下一测试预期RED；不提交、不注册。

#### File 8 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/test/java/top/egon/cola/platform/tianquan/jianshen/starter/field/Rbac3FieldWriteAuthorizationComponentTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: Rbac3FieldWriteAuthorizationComponentTest；explicit_write_field_contract。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，原DefaultAuthorizationService/CurrentRbac3User/field read与method fixtures，真实generated Mapper/Validator是现成邻近能力。
- Dependencies and consumers: 原DefaultAuthorizationService/CurrentRbac3User/field read与method fixtures，真实generated Mapper/Validator；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 新增/扩充explicit_write_field_contract方法；TEST-017–021受控行为断言，默认不启PG/Redis/Testcontainers。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: 一个PDP ALLOW全部WRITE通过；READ/MASKED/NONE/缺项/DENY拒绝spy0；INDETERMINATE503；SERVICE/无USER拒绝；null presence必须检查；旧读JSON不变/HV000151无
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void explicit_write_field_contract() {
    arrange 复用原DefaultAuthorizationService/CurrentRbac3User/field read与method fixtures，真实generated Mapper/Validator，不启动应用或外部基础设施;
    act 调用该Step实际生产入口，不以手工调用record()/mock整段授权替代真实分支;
    assert 一个PDP ALLOW全部WRITE通过；READ/MASKED/NONE/缺项/DENY拒绝spy0；INDETERMINATE503；SERVICE/无USER拒绝；null presence必须检查；旧读JSON不变/HV000151无;
    verify 所有拒绝/不可用分支businessSpy为零且敏感值不进入event/labels/JSON;
}
```

- Verification contribution: 本Step `Rbac3FieldWriteAuthorizationComponentTest,DefaultAuthorizationServiceTest,Rbac3FieldPropertyWriterTest,Rbac3MethodAuthorizationManagerTest`；一个PDP ALLOW全部WRITE通过；READ/MASKED/NONE/缺项/DENY拒绝spy0；INDETERMINATE503；SERVICE/无USER拒绝；null presence必须检查；旧读JSON不变/HV000151无 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 9 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/field/Rbac3FieldWriteAuthorizationComponent.java`

- Purpose: schema→单次PDP→逐项WRITE，拒绝/不可用不执行业务；本次只完成本Step所属切片。
- Symbols: requireWritableFields；Rbac3FieldWriteAuthorizationComponent。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，qualified authorizationService/currentRbac3User/fieldWriteAuthorizationConverterImpl/defaultValidator；原field read与method PEP均不修改是现成邻近能力。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: qualified authorizationService/currentRbac3User/fieldWriteAuthorizationConverterImpl/defaultValidator；原field read与method PEP均不修改；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: FieldPolicyDecision requireWritableFields(@Valid FieldWriteAuthorizationCommand command)，@Validated(WriteFields.class)。
- Input/output and state mapping: WriteFields schema→currentRbac3User.require().identity→generated request/default校验→一次authorizationService.decideFields→decision default/cascade→overall ALLOW且每项WRITE→原decision。
- Error and edge behavior: 无USER/SERVICE用现有AuthenticationCredentialsNotFoundException拒绝，不伪造IdentityPrincipal；DENY/任一缺字段或非WRITE→org.springframework.security.authorization.AuthorizationDeniedException("FIELD_ACCESS_DENIED")；INDETERMINATE→RuntimeUnavailableException(reason,identity)；所有失败0业务写。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
@Slf4j @RequiredArgsConstructor @Validated(FieldWriteAuthorizationCommand.WriteFields.class)
class Rbac3FieldWriteAuthorizationComponent { @Qualifier("authorizationService") final AuthorizationService authorization; @Qualifier("currentRbac3User") final CurrentRbac3User currentUser; @Qualifier("fieldWriteAuthorizationConverterImpl") final FieldWriteAuthorizationConverter converter; @Qualifier("defaultValidator") final Validator validator;
    requireWritableFields(command) { new ValidationUtils(validator).validate(command,WriteFields.class); identity=currentUser.require().identity();
        request=new ValidationUtils(validator).validate(converter.toTarget(command)); decision=new ValidationUtils(validator).validate(authorization.decideFields(request));
        INDETERMINATE→原RuntimeUnavailableException；overall!=ALLOW或submittedFields任一缺失/level!=WRITE→原FIELD_ACCESS_DENIED；return decision;
    } // 不查业务库，不执行事务/写入，不改DTO；fence由原PDP判断
}
```

- Verification contribution: 本Step `Rbac3FieldWriteAuthorizationComponentTest,DefaultAuthorizationServiceTest,Rbac3FieldPropertyWriterTest,Rbac3MethodAuthorizationManagerTest`；无USER/SERVICE用现有AuthenticationCredentialsNotFoundException拒绝，不伪造IdentityPrincipal；DENY/任一缺字段或非WRITE→org.springframework.security.authorization.AuthorizationDeniedException("FIELD_ACCESS_DENIED")；INDETERMINATE→RuntimeUnavailableException(reason,identity)；所有失败0业务写。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 10 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/lombok.config`

- Purpose: 仅新/改业务Bean注入规范；不重构既有全域；本次只完成本Step所属切片。
- Symbols: copyable Qualifier；lombok。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，Lombok1.18.46；JS/JA annotated business Components；自身Bean注入测试是现成邻近能力。
- Dependencies and consumers: Lombok1.18.46；JS/JA annotated business Components；自身Bean注入测试；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 新增子平台lombok.config供JS/JA新改业务Bean复制Qualifier。
- Input/output and state mapping: config.stopBubbling=true；copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier。
- Error and edge behavior: 只编译已改Bean，不全域注入重构；检查无更近配置遮盖，Shoubing各模块已有同项不重写。
- Standards impact: MC-CONFIG-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001：配置/lock是数据文件，Java POJO/日志注解不适用；保持同构/真实制品校验和当前owner，无secret值。
- Literal rule enforcement: Rule 7, Rule 11：同核心key或既有artifact声明不变，范围仅本平台；Rule 4：Qualifier复制到Lombok构造参数。
- Implementation pseudocode:

```text
config.stopBubbling = true
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
# 新改业务Bean的final qualified fields经@RequiredArgsConstructor复制到构造参数；编译和容器测试证明。
```

- Verification contribution: 本Step `Rbac3FieldWriteAuthorizationComponentTest,DefaultAuthorizationServiceTest,Rbac3FieldPropertyWriterTest,Rbac3MethodAuthorizationManagerTest`；只编译已改Bean，不全域注入重构；检查无更近配置遮盖，Shoubing各模块已有同项不重写。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 11 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/autoconfigure/Rbac3StarterAutoConfiguration.java`

- Purpose: 原authorizationService Bean名保留，named delegate/observer/field converter；本次只完成本Step所属切片。
- Symbols: authorizationService/guard注册；Rbac3StarterAutoConfiguration。
- Repository evidence: 当前路径存在，已有authorizationService/guard注册；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 旧method interceptor/field module/current user；新guard/observer；Idp auto configure先执行；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 原authorizationService Bean名与所有known callers保持；新增具名field converter/guard与enabled PDP delegate观察。
- Input/output and state mapping: Step2注册guard→原authorizationService；Step9默认实现改具名rbac3AuthorizationDelegate，外露authorizationService按开关wrap；原consumer customBean替代语义保留。
- Error and edge behavior: enabled不允许PDP recorder/producer缺失silent noop；disabled回原路径；避免对delegate自身再次wrap/同type歧义；Lombok Qualified注入通过JS parent config。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
复用SS原VerifiedPrincipalConverterConfiguration的嵌套配置+@ComponentScan模式，JS新增内部扫描仅FieldWriteAuthorizationConverter所在包以注册generated Impl;
@Bean(name="rbac3FieldWriteAuthorizationComponent") factory参数@Qualifier("authorizationService"/"currentRbac3User"/"fieldWriteAuthorizationConverterImpl"/"defaultValidator");
返回@RequiredArgsConstructor生成的Component，原authorizationService factory与ConditionalOnMissingBean替代语义保持;
// 当前不引用Step9 ObservedAuthorizationService；不靠host扫描外模块来假设mapper自动可用。
```

- Verification contribution: 本Step `Rbac3FieldWriteAuthorizationComponentTest,DefaultAuthorizationServiceTest,Rbac3FieldPropertyWriterTest,Rbac3MethodAuthorizationManagerTest`；enabled不允许PDP recorder/producer缺失silent noop；disabled回原路径；避免对delegate自身再次wrap/同type歧义；Lombok Qualified注入通过JS parent config。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`（Maven）；Step11 npm用下列完整 --prefix，无服务进程。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter -am -Dtest=Rbac3FieldWriteAuthorizationComponentTest,DefaultAuthorizationServiceTest,Rbac3FieldPropertyWriterTest,Rbac3MethodAuthorizationManagerTest -Dsurefire.failIfNoSpecifiedTests=false test`；该Step `git diff --check` 与精确commit path审查。
- Expected result: RED必须是上述缺失行为断言；GREEN exit0、目标命名tests实际执行且0 failures/errors/非预期skip；无源schema、Bean注入/敏感值或旧机制退化。Step11普通解析不得走临时alias。
- Failure returns to: 本Step对应assert的生产File；schema/DI到对应载体/factory，receipt状态到Step3/4，原合同退化停止当前Step；artifact缺失停止Step11等待可信输入；若需加库/改SQL/外platform或改Spec语义，回用户/Spec。
- Completion criteria: 平台guard单次PDP+全WRITE成立，三维旧请求/读serializer兼容；没有业务接口自动接入；保存实际RED/GREEN命令、测试report/准确数量、适用MC证据及当前HEAD，没有业务未完成签名/placeholder或外部runtime假成功。
- Rollback: 未部署时只还原本Step相对前置HEAD的精确diff，保留之前Step及未归属改动；已产生审计/spool时禁止删帧/回旧consumer，按§9停止入口并forward-fix。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/authorization/AuthorizationService.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-contract/src/main/java/top/egon/cola/platform/tianquan/jianshen/contract/authorization/FieldPolicyDecision.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/field/FieldWriteAuthorizationCommand.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/field/FieldWriteAuthorizationConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/field/Rbac3FieldWriteAuthorizationComponent.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/test/java/top/egon/cola/platform/tianquan/jianshen/starter/field/Rbac3FieldWriteAuthorizationComponentTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/lombok.config`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/autoconfigure/Rbac3StarterAutoConfiguration.java`., `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-contract/pom.xml`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/pom.xml`
- Commit: `feat(jianshen): add explicit field write authorization`；本轮只规划，不执行。


### Step 3 — 把严格审计持久化设为放行前置

- Requirements: REQ-001, REQ-006, REQ-008, REQ-014, REQ-018
- Dependencies: None；基线HEAD与原目标tests需先记录
- Baseline state: 当前基线缺口：appendStrict编译前置仅委托旧append：stage/fsync失败仍可broker-only返回，atomic fallback不拒绝
- Observable outcome: 把严格审计持久化设为放行前置；以本Step命名真实行为断言证明，错误分支和负作用同时核对。
- End state: strict stage/force/atomic/dir durability+MQ失败行为受控；旧append未被全host隐式开启。
- Test-first gate: Required — appendStrict编译前置仅委托旧append：stage/fsync失败仍可broker-only返回，atomic fallback不拒绝；按File顺序签名编译前置后才运行RED，编译/fixture/环境失败不计。
- Manual Checks: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001；按每File Standards impact及本Step验证分别闭合。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`

- Purpose: 同Step编译前置：原spool/Streams增强strict持久/receipt/有界重放，不删远端目录；仅建立兼容签名/原委托，暂不注册/启用，后续同路径完成GREEN。
- Symbols: appendStrict / stageStrict / force file+directory / 原兼容factory；TokenUsageAuditComponent。
- Repository evidence: 当前路径存在，已有appendStrict/reference/confirm/replayPending；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 原Redisson/Jackson/OAuthAuditProperties/现有Validator；原append调用者；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 当前测试所需契约尚不存在，先建立仅原委托的可编译入口；后续立即真实RED。
- Contract/signature changes: 先加仅委托append的编译前置，RED后实现appendStrict与strict stage；不涉及Step4 receipt/DTO/Props。
- Input/output and state mapping: strict先校验Event→规范内容冲突检查→file force→ATOMIC_MOVE→directory force→XADD确认→SENT marker force；receipt验证后本节点清理。
- Error and edge behavior: strict不允许AtomicMove fallback或stage错误后broker-only返回；XADD/mark失败保留帧并抛原OAUTH_AUDIT_*；STORED/EXPIRED可清理，DEAD隔离计容量；旧sent等时重投同id。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
// 同Step签名编译前置；不代表GREEN，暂不启用生产Bean或独立提交。
先加仅委托append的编译前置，RED后实现appendStrict与strict stage；不涉及Step4 receipt/DTO/Props。
// observe/requireWritableFields/appendStrict仅委托原验证/PDP/append；metric record暂不发信号，配置只声明具名factory。
// 后续测试必须因缺失字段WRITE/strict durability/usage/metrics行为失败，再回本文件按完整pseudocode实现。
```

- Verification contribution: 本Step `TokenUsageAuditComponentTest`；只满足编译，不能代替下一RED验证。
- After this file: 签名可编译但行为尚缺，下一测试预期RED；不提交、不注册。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponentTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: strict_file_atomic_directory_and_broker_failure；strict_spool_and_receipt_recovery。
- Repository evidence: 当前路径存在，已有TokenUsageAuditComponentTest；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 既有TokenUsageAuditComponentTest temp directory、真实component和Redisson test doubles；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 只新增TEST009/010 strict durability、XADD未知/markSent失败断言；receipt/restart cursor在Step4扩充。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: strict文件force/目录force/atomic不支持/满容量失败必须拒绝且broker零调用；XADD/mark失败保留原id帧可恢复。
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void strict_file_atomic_directory_and_broker_failure() {
    component=真实TokenUsageAuditComponent(tempSpool,原key,受控RStream);
    用component内部现有/本Step package-private文件操作故障接缝模拟file force/atomic/dir force失败；assertThrows原OAUTH_AUDIT_*;
    verify(stream,never()).add(any()); assert既有未确认帧不删;
    再模拟XADD未知或markSent失败，assert失败且事件ID/内容的evt保留；恢复存储后原replayPending重投同id;
}
```

- Verification contribution: 本Step `TokenUsageAuditComponentTest`；strict文件force/目录force/atomic不支持/满容量失败必须拒绝且broker零调用；XADD/mark失败保留原id帧可恢复。 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`

- Purpose: 原spool/Streams增强strict持久/receipt/有界重放，不删远端目录；本次只完成本Step所属切片。
- Symbols: appendStrict / stageStrict / force file+directory / 原兼容factory；TokenUsageAuditComponent。
- Repository evidence: 当前路径存在，已有appendStrict/reference/confirm/replayPending；保留相邻注解/工厂/原异常风格。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 原Redisson/Jackson/OAuthAuditProperties/现有Validator；原append调用者；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 先加仅委托append的编译前置，RED后实现appendStrict与strict stage；不涉及Step4 receipt/DTO/Props。
- Input/output and state mapping: strict先校验Event→规范内容冲突检查→file force→ATOMIC_MOVE→directory force→XADD确认→SENT marker force；receipt验证后本节点清理。
- Error and edge behavior: strict不允许AtomicMove fallback或stage错误后broker-only返回；XADD/mark失败保留帧并抛原OAUTH_AUDIT_*；STORED/EXPIRED可清理，DEAD隔离计容量；旧sent等时重投同id。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
appendStrict(event) { new ValidationUtils(defaultValidator).validate(event);
    同id内容冲突拒绝；stageStrict原frame临时文件force(true)→ATOMIC_MOVE（不支持就失败）→目录force(true);
    messageId=原publish(event); require实际XADD确认；markSent后force标记与目录；return messageId;
}
// 任一步失败保存可恢复帧并抛原OAUTH_AUDIT_*，不得stage失败后尝试broker-only成功；原append/factory保留。
// 本Step不引用任何尚未存在的receipt类型、Core metrics port或重放确认行为。
```

- Verification contribution: 本Step `TokenUsageAuditComponentTest`；strict不允许AtomicMove fallback或stage错误后broker-only返回；XADD/mark失败保留帧并抛原OAUTH_AUDIT_*；STORED/EXPIRED可清理，DEAD隔离计容量；旧sent等时重投同id。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`（Maven）；Step11 npm用下列完整 --prefix，无服务进程。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=TokenUsageAuditComponentTest -Dsurefire.failIfNoSpecifiedTests=false test`；该Step `git diff --check` 与精确commit path审查。
- Expected result: RED必须是上述缺失行为断言；GREEN exit0、目标命名tests实际执行且0 failures/errors/非预期skip；无源schema、Bean注入/敏感值或旧机制退化。Step11普通解析不得走临时alias。
- Failure returns to: 本Step对应assert的生产File；schema/DI到对应载体/factory，receipt状态到Step3/4，原合同退化停止当前Step；artifact缺失停止Step11等待可信输入；若需加库/改SQL/外platform或改Spec语义，回用户/Spec。
- Completion criteria: strict stage/force/atomic/dir durability+MQ失败行为受控；旧append未被全host隐式开启；保存实际RED/GREEN命令、测试report/准确数量、适用MC证据及当前HEAD，没有业务未完成签名/placeholder或外部runtime假成功。
- Rollback: 未部署时只还原本Step相对前置HEAD的精确diff，保留之前Step及未归属改动；已产生审计/spool时禁止删帧/回旧consumer，按§9停止入口并forward-fix。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponentTest.java`.
- Commit: `fix(shoubing): require durable audit handoff`；本轮只规划，不执行。


### Step 4 — 闭合跨节点审计确认与有界重放

- Requirements: REQ-001, REQ-002, REQ-006, REQ-007, REQ-008, REQ-014, REQ-015, REQ-018
- Dependencies: Step 3 已验证并提交
- Baseline state: 前置Step合同已GREEN并提交；本Step缺口：旧consumer先XACK且没有receipt；旧acknowledge凭eventId删文件，旧replay跳过.sent；双目录/伪确认/timeout断言失败
- Observable outcome: 闭合跨节点审计确认与有界重放；以本Step命名真实行为断言证明，错误分支和负作用同时核对。
- End state: consumer原persist完成→receipt→XACK；sender核对后清本目录，DEAD/EXPIRED与循环cursor可恢复；self producer尚未启用。
- Test-first gate: Required — 旧consumer先XACK且没有receipt；旧acknowledge凭eventId删文件，旧replay跳过.sent；双目录/伪确认/timeout断言失败；按File顺序签名编译前置后才运行RED，编译/fixture/环境失败不计。
- Manual Checks: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001；按每File Standards impact及本Step验证分别闭合。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/AuditDeliveryProperties.java`

- Purpose: 有界确认/重放参数；原OAuthAuditProperties四字段不改；本次只完成本Step所属切片。
- Symbols: TTL/timeout/batch；AuditDeliveryProperties。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，Boot binder/TokenUsageAuditComponent/consumer；原audit Properties四参调用者兼容是现成邻近能力。
- Dependencies and consumers: Boot binder/TokenUsageAuditComponent/consumer；原audit Properties四参调用者兼容；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: @ConfigurationProperties("egon.cola.platform.tianquan.shoubing.audit.delivery") class AuditDeliveryProperties。
- Input/output and state mapping: receiptTtl=PT1H（1m..24h）；confirmationTimeout=PT30S（5s..5m）；batchSize=100（1..1000）。
- Error and edge behavior: Duration非null、有界；保持OAuthAuditProperties旧四字段不变；定时器沿旧5000ms，不新增job。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001：普通class必须@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder；根class不callSuper；字段annotations/default/groups见上述contract。 无业务DI；Date禁用，JSON采用Boot Jackson。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：上述精确角色、约束/建模与UTC Duration/Instant；URI及hash不可变；不另建模型/新层。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder @Validated
@ConfigurationProperties("egon.cola.platform.tianquan.shoubing.audit.delivery")
class AuditDeliveryProperties { @NotNull @Builder.Default Duration receiptTtl=Duration.ofHours(1); @NotNull @Builder.Default Duration confirmationTimeout=Duration.ofSeconds(30); @Min(1) @Max(1000) @Builder.Default int batchSize=100; }
@AssertTrue boolean durationsInBounds() { /* native Duration边界检查，不使用Date */ }
```

- Verification contribution: 本Step `TokenUsageAuditComponentTest,OAuthAuditConsumerTest,TokenUsageAuditReceiptContractTest`；Duration非null、有界；保持OAuthAuditProperties旧四字段不变；定时器沿旧5000ms，不新增job。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/domain/dto/TokenUsageAuditReceiptDTO.java`

- Purpose: eventId/指纹/outcome/时间/MAC的严格技术确认；本次只完成本Step所属切片。
- Symbols: receipt schema1；TokenUsageAuditReceiptDTO。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，原Boot Jackson/Validator；producer/consumer技术codec是现成邻近能力。
- Dependencies and consumers: 原Boot Jackson/Validator；producer/consumer技术codec；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: class TokenUsageAuditReceiptDTO六字段，@JsonPropertyOrder固定序；schemaVersion int=1。
- Input/output and state mapping: eventId/fingerprint/outcome/confirmedAt/signature规范UTF8长度前缀/HMAC；ISO UTC Instant；strict Jackson reader。
- Error and edge behavior: @Min(1) @Max(1)、eventId正则≤64、hash64、outcome三种、@NotNull时间；unknown field/坏签名拒绝而不删spool。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001：普通class必须@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder；根class不callSuper；字段annotations/default/groups见上述contract。 无业务DI；Date禁用，JSON采用Boot Jackson。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：上述精确角色、约束/建模与UTC Duration/Instant；URI及hash不可变；不另建模型/新层。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder
@JsonPropertyOrder({"schemaVersion","eventId","eventFingerprint","outcome","confirmedAt","signature"})
class TokenUsageAuditReceiptDTO {
    @Min(1) @Max(1) int schemaVersion;
    @NotBlank @Pattern(regexp="[A-Za-z0-9._~-]{1,64}") String eventId;
    @NotBlank @Pattern(regexp="[0-9a-f]{64}") String eventFingerprint;
    @NotBlank @Pattern(regexp="STORED|EXPIRED|DEAD") String outcome;
    @NotNull Instant confirmedAt;
    @NotBlank @Pattern(regexp="[0-9a-f]{64}") String signature;
}
// objectMapper.readerFor(DTO).with(FAIL_ON_UNKNOWN_PROPERTIES)，不修改全局Jackson配置；signature不输出key。
```

- Verification contribution: 本Step `TokenUsageAuditComponentTest,OAuthAuditConsumerTest,TokenUsageAuditReceiptContractTest`；@Min(1) @Max(1)、eventId正则≤64、hash64、outcome三种、@NotNull时间；unknown field/坏签名拒绝而不删spool。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/TokenUsageAuditReceiptContractTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: TokenUsageAuditReceiptContractTest；independent_spools_confirm_verified_fact。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，真实producer/component/consumer codec与两个temp dirs、受控RStream/RBucket Clock是现成邻近能力。
- Dependencies and consumers: 真实producer/component/consumer codec与两个temp dirs、受控RStream/RBucket Clock；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 新增/扩充independent_spools_confirm_verified_fact方法；TEST-011/013/014受控行为断言，默认不启PG/Redis/Testcontainers。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: 同事实STORED只删正确sender；伪MAC/id/fingerprint/schema/unknown字段保留；TTL失/XACK故障同id重投；无跨volume删除
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void independent_spools_confirm_verified_fact() {
    arrange 复用真实producer/component/consumer codec与两个temp dirs、受控RStream/RBucket Clock，不启动应用或外部基础设施;
    act 调用该Step实际生产入口，不以手工调用record()/mock整段授权替代真实分支;
    assert 同事实STORED只删正确sender；伪MAC/id/fingerprint/schema/unknown字段保留；TTL失/XACK故障同id重投；无跨volume删除;
    verify 所有拒绝/不可用分支businessSpy为零且敏感值不进入event/labels/JSON;
}
```

- Verification contribution: 本Step `TokenUsageAuditComponentTest,OAuthAuditConsumerTest,TokenUsageAuditReceiptContractTest`；同事实STORED只删正确sender；伪MAC/id/fingerprint/schema/unknown字段保留；TTL失/XACK故障同id重投；无跨volume删除 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumerTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: OAuthAuditConsumerTest；persist_receipt_before_xack。
- Repository evidence: 当前路径存在，已有OAuthAuditConsumerTest；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 现有OAuthAuditConsumerTest Mockito/原save返回与issuer fixtures；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 新增/扩充persist_receipt_before_xack方法；TEST-012/015受控行为断言，默认不启PG/Redis/Testcontainers。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: InOrder 原save成功/同id核对→receipt SET→XACK；rollback/unknown无STORED；180天EXPIRED不重建；confirmed DEAD不清frame；null refs signed必验
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void persist_receipt_before_xack() {
    arrange 复用现有OAuthAuditConsumerTest Mockito/原save返回与issuer fixtures，不启动应用或外部基础设施;
    act 调用该Step实际生产入口，不以手工调用record()/mock整段授权替代真实分支;
    assert InOrder 原save成功/同id核对→receipt SET→XACK；rollback/unknown无STORED；180天EXPIRED不重建；confirmed DEAD不清frame；null refs signed必验;
    verify 所有拒绝/不可用分支businessSpy为零且敏感值不进入event/labels/JSON;
}
```

- Verification contribution: 本Step `TokenUsageAuditComponentTest,OAuthAuditConsumerTest,TokenUsageAuditReceiptContractTest`；InOrder 原save成功/同id核对→receipt SET→XACK；rollback/unknown无STORED；180天EXPIRED不重建；confirmed DEAD不清frame；null refs signed必验 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponentTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: TokenUsageAuditComponentTest；strict_spool_and_receipt_recovery。
- Repository evidence: 当前路径存在，已有TokenUsageAuditComponentTest；保留相邻注解/工厂/原异常风格。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 既有TokenUsageAuditComponentTest temp directory、真实component和Redisson test doubles；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 新增/扩充strict_spool_and_receipt_recovery方法；TEST-009/010/016受控行为断言，默认不启PG/Redis/Testcontainers。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: file/dir force与atomic失败503且broker0；XADD未知保同id；bad receipt零删、SENT超时重放、batch cursor不饥饿、restart/DEAD容量
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void strict_spool_and_receipt_recovery() {
    arrange 复用既有TokenUsageAuditComponentTest temp directory、真实component和Redisson test doubles，不启动应用或外部基础设施;
    act 调用该Step实际生产入口，不以手工调用record()/mock整段授权替代真实分支;
    assert file/dir force与atomic失败503且broker0；XADD未知保同id；bad receipt零删、SENT超时重放、batch cursor不饥饿、restart/DEAD容量;
    verify 所有拒绝/不可用分支businessSpy为零且敏感值不进入event/labels/JSON;
}
```

- Verification contribution: 本Step `TokenUsageAuditComponentTest,OAuthAuditConsumerTest,TokenUsageAuditReceiptContractTest`；file/dir force与atomic失败503且broker0；XADD未知保同id；bad receipt零删、SENT超时重放、batch cursor不饥饿、restart/DEAD容量 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`

- Purpose: 原spool/Streams增强strict持久/receipt/有界重放，不删远端目录；本次只完成本Step所属切片。
- Symbols: receipt codec / acknowledge / replayPending cursor；TokenUsageAuditComponent。
- Repository evidence: 当前路径存在，已有appendStrict/reference/confirm/replayPending；保留相邻注解/工厂/原异常风格。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 新AuditDeliveryProperties/TokenUsageAuditReceiptDTO、原Clock/Jackson/Redisson；无Core metrics依赖；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 在Step3 strict基础上增加fingerprint/signReceipt/receipt验证与acknowledge/replay；旧append及已完成durability不回退。
- Input/output and state mapping: strict先校验Event→规范内容冲突检查→file force→ATOMIC_MOVE→directory force→XADD确认→SENT marker force；receipt验证后本节点清理。
- Error and edge behavior: strict不允许AtomicMove fallback或stage错误后broker-only返回；XADD/mark失败保留帧并抛原OAUTH_AUDIT_*；STORED/EXPIRED可清理，DEAD隔离计容量；旧sent等时重投同id。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
fingerprint(event) { 按原11字段顺序，UTF8字段int32 big-endian长度前缀，null为-1，Instant.toString/issuer.toASCIIString→SHA256; }
signReceipt(receipt) { HMAC原挂载key，raw固定audit-receipt-v1前缀+schemaVersion/eventId/fingerprint/outcome/confirmedAt同长度编码; }
acknowledge(eventId) { 本目录frame→strict Jackson DTO/schema→eventId/fingerprint+MessageDigest.isEqual(MAC)；STORED/EXPIRED删本节点evt/sent并force目录，DEAD原帧隔离保留计容量; }
replayPending() { 确定cursor按batch轮询；先确认，未确认STAGED或SENT超过30s同id重投；没有receipt/坏确认均零删除，不饥饿; }
// 保持实际eventPath=<eventId>.evt与sentPath=<eventId>.evt.sent；旧SENT仅ASCII StreamMessageId，新旧均不改变frame schema。
// SENT时间取marker mtime；新handoff按Clock更新时间并force metadata；超timeout重投成功原子替换marker/mtime，不能因exists提前return。
// DEAD使用<eventId>.evt.dead受验证receipt标记，原evt保留；有界扫描处理缺evt的孤立sent marker，不删除任何未确认事实。
// 原frame/schema1可读、原5000ms job不新增；Clock/Delivery/Validator参数由Lombok factory扩充且旧3参适配保持。
```

- Verification contribution: 本Step `TokenUsageAuditComponentTest,OAuthAuditConsumerTest,TokenUsageAuditReceiptContractTest`；strict不允许AtomicMove fallback或stage错误后broker-only返回；XADD/mark失败保留帧并抛原OAUTH_AUDIT_*；STORED/EXPIRED可清理，DEAD隔离计容量；旧sent等时重投同id。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 7 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumer.java`

- Purpose: persist返回后签名receipt再XACK；DEAD/EXPIRED区别；停止消费者代删sender目录；本次只完成本Step所属切片。
- Symbols: process/parse/receipt；OAuthAuditConsumer。
- Repository evidence: 当前路径存在，已有process/parse/receipt；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 原OAuthAuditRepository.save/findByEventId、TenantIssuerService、Clock、TokenUsageAuditComponent codec、新delivery Props；旧Outbox unsigned保持；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: process/parse/publishReceipt及dead处理；停止durableAudit.acknowledge代删sender。
- Input/output and state mapping: parse已签usage含null tokenRef→原persist/save返回或同eventId事实核对→STORED receipt SET TTL→XACK；旧180天→EXPIRED；dead确认→DEAD。
- Error and edge behavior: DBrollback/commit未知不发STORED不XACK；借用已有save返回/现有findByEventId核对字段，不增SQL；unknown/invalid不可构造完整事件不发伪receipt；receipt/XACK失败原id重试。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
process(stream,messages) {
    event = 原parse且present signature必验（即使tokenRef null）;
    outcome = event.olderThan180days ? EXPIRED : 原幂等persist与内容核对成功的STORED;
    if (signedUsage) publishReceipt(经ValidationUtils与规范11字段fingerprint/MAC的DTO);
    stream.ack(GROUP,id); 原attempts.delete(); // 不删除consumer自身或远端producer文件
}
// retries耗尽：先确认原deadStream，再对可重建的可信usage发布DEAD；frame由sender隔离保留。
```

- Verification contribution: 本Step `TokenUsageAuditComponentTest,OAuthAuditConsumerTest,TokenUsageAuditReceiptContractTest`；DBrollback/commit未知不发STORED不XACK；借用已有save返回/现有findByEventId核对字段，不增SQL；unknown/invalid不可构造完整事件不发伪receipt；receipt/XACK失败原id重试。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 8 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java`

- Purpose: 显式named/qualified装配，disabled不增加依赖；自身开启缺能力失败；本次只完成本Step所属切片。
- Symbols: OAuthAuditConfiguration delivery/Validator/Clock factory；IdpStarterAutoConfiguration。
- Repository evidence: 当前路径存在，已有观察/producer组件Beans；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 原OAuthAuditProperties/qualified rbac3RuntimeRedissonClient/jacksonObjectMapper/idpClock/defaultValidator；新Delivery Properties；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 本Step只扩OAuthAuditConfiguration注册auditDeliveryProperties及TokenUsageAuditComponent必要参数；原typed verifier装配不变。
- Input/output and state mapping: managed IdpJwtVerifier createConfigured观察组合→旧final USER/SERVICE包装器及HTTP combined→RPC；disabled仍原构造。
- Error and edge behavior: 开启要求真实producer/converter/Validator/host recorder；缺能力失败，不能ConditionalOnBean静默跳过安全能力；避免shared spy造成循环；Servlet filter仅在chain内启用一次。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
@Bean(name="auditDeliveryProperties") @ConfigurationProperties固定audit.delivery prefix，验证typed Duration和batch范围;
@Bean(name="tokenUsageAuditComponent") factory追加具名auditDeliveryProperties/idpClock/defaultValidator，依赖均@Qualifier;
返回Lombok required-args组件；旧公开3参技术构造委托默认Delivery/Clock且不改其他调用者;
// 不引用尚未创建的observer/usage properties/metrics；自身usage尚未启用。
```

- Verification contribution: 本Step `TokenUsageAuditComponentTest,OAuthAuditConsumerTest,TokenUsageAuditReceiptContractTest`；开启要求真实producer/converter/Validator/host recorder；缺能力失败，不能ConditionalOnBean静默跳过安全能力；避免shared spy造成循环；Servlet filter仅在chain内启用一次。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`（Maven）；Step11 npm用下列完整 --prefix，无服务进程。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenUsageAuditComponentTest,OAuthAuditConsumerTest,TokenUsageAuditReceiptContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；该Step `git diff --check` 与精确commit path审查。
- Expected result: RED必须是上述缺失行为断言；GREEN exit0、目标命名tests实际执行且0 failures/errors/非预期skip；无源schema、Bean注入/敏感值或旧机制退化。Step11普通解析不得走临时alias。
- Failure returns to: 本Step对应assert的生产File；schema/DI到对应载体/factory，receipt状态到Step3/4，原合同退化停止当前Step；artifact缺失停止Step11等待可信输入；若需加库/改SQL/外platform或改Spec语义，回用户/Spec。
- Completion criteria: consumer原persist完成→receipt→XACK；sender核对后清本目录，DEAD/EXPIRED与循环cursor可恢复；self producer尚未启用；保存实际RED/GREEN命令、测试report/准确数量、适用MC证据及当前HEAD，没有业务未完成签名/placeholder或外部runtime假成功。
- Rollback: 未部署时只还原本Step相对前置HEAD的精确diff，保留之前Step及未归属改动；已产生审计/spool时禁止删帧/回旧consumer，按§9停止入口并forward-fix。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/AuditDeliveryProperties.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/domain/dto/TokenUsageAuditReceiptDTO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/TokenUsageAuditReceiptContractTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumerTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponentTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumer.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java`.
- Commit: `fix(shoubing): confirm audit delivery across nodes`；本轮只规划，不执行。


### Step 5 — 从真实验签和缓存分支发出安全观测

- Requirements: REQ-001, REQ-002, REQ-009, REQ-010, REQ-011, REQ-014, REQ-018
- Dependencies: None；基线HEAD与原目标tests需先记录
- Baseline state: 当前基线缺口：编译前置只增加兼容观察overload并委托原流程，真实验签/cache执行后recording port样本仍为零；不是编译失败
- Observable outcome: 从真实验签和缓存分支发出安全观测；以本Step命名真实行为断言证明，错误分支和负作用同时核对。
- End state: Core纯port与真实密码学/JWK/SERVICE信号可验证；旧构造/factory/key/单飞兼容；host接线尚未启用。
- Test-first gate: Required — 编译前置只增加兼容观察overload并委托原流程，真实验签/cache执行后recording port样本仍为零；不是编译失败；按File顺序签名编译前置后才运行RED，编译/fixture/环境失败不计。
- Manual Checks: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001；按每File Standards impact及本Step验证分别闭合。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/observability/SecurityMetricObservationBO.java`

- Purpose: 有界技术观测值，默认组校验；本次只完成本Step所属切片。
- Symbols: signal/result/duration/value；SecurityMetricObservationBO。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，原Core validation/common；IdpSecurityMetricsRecorder / 两host是现成邻近能力。
- Dependencies and consumers: 原Core validation/common；IdpSecurityMetricsRecorder / 两host；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: class SecurityMetricObservationBO：String signal/result，Duration duration，long value。
- Input/output and state mapping: 九个固定signal→对应Timer/Counter/Gauge；duration仅Timer、value仅Gauge；无持久化。
- Error and edge behavior: @NotBlank/@Pattern有限集合，@PositiveOrZero value；@AssertTrue校验Timer duration非负且必填、Counter/Gauge形状；不自动接收新signal。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001：普通class必须@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder；根class不callSuper；字段annotations/default/groups见上述contract。 无业务DI；Date禁用，JSON采用Boot Jackson。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：上述精确角色、约束/建模与UTC Duration/Instant；URI及hash不可变；不另建模型/新层。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
class SecurityMetricObservationBO { @NotBlank String signal; @NotBlank String result; Duration duration; @PositiveOrZero long value; }
@AssertTrue boolean isShapeValid() { /* 固定timer集合要求duration != null且非负；counter/gauge拒绝额外duration */ }
// 信号固定为Spec §7.3.5九项；结果按预先定义的有限分类校验。
```

- Verification contribution: 本Step `MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest`；@NotBlank/@Pattern有限集合，@PositiveOrZero value；@AssertTrue校验Timer duration非负且必填、Counter/Gauge形状；不自动接收新signal。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/observability/IdpSecurityMetricsRecorder.java`

- Purpose: 纯JDK指标port，无Micrometer/HTTP/身份标签；本次只完成本Step所属切片。
- Symbols: record；IdpSecurityMetricsRecorder。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，SecurityMetricObservationBO；SS verifier/cache与JS PDP，两个Admin adapter是现成邻近能力。
- Dependencies and consumers: SecurityMetricObservationBO；SS verifier/cache与JS PDP，两个Admin adapter；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: void record(@Valid SecurityMetricObservationBO observation)，仅此操作；Core不出现Micrometer类型。
- Input/output and state mapping: 真实signal/result/Duration/value输入→void；不接收tags/身份，host决定receiver。
- Error and edge behavior: 默认noop仅用于disabled；自身enabled必须由真实host实现；override不强化接口约束。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-ARCH-001：接口仅契约/原nested record metadata，无具体业务Bean注入或日志；default约束不强化override参数，纯Core不反向依赖host。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11：精确角色/原三维record或单operation接口，注解语义等同原constructor；不扩public参数。
- Implementation pseudocode:

```java
public interface IdpSecurityMetricsRecorder {
    void record(@Valid SecurityMetricObservationBO observation);
}
// 两Admin实现此port；SS/JS只持有port，不向Core反向导入Spring Admin。
```

- Verification contribution: 本Step `MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest`；默认noop仅用于disabled；自身enabled必须由真实host实现；override不强化接口约束。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java`

- Purpose: 同Step编译前置：真实JWT/JWK分支观察，保留2/3参构造和旧resolver接口（Edge调用保持）；仅建立兼容签名/原委托，暂不注册/启用，后续同路径完成GREEN。
- Symbols: authenticate(String compactToken) / manager builder / BoundedJwkSetCache；TenantIssuerAuthenticationManagerComponent。
- Repository evidence: 当前路径存在，已有authenticate观察overload/BoundedJwkSetCache；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 原TrustedIssuerRegistry/IdpStarterProperties/Clock；Core recorder固定绑定cache与Nimbus processor；Edge旧构造noop兼容；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 当前测试所需契约尚不存在，先建立仅原委托的可编译入口；后续立即真实RED。
- Contract/signature changes: 原VerifiedJwtBO authenticate(String compactToken)及2/3参构造保持；Lombok factory可追加固定Core recorder，新的观察overload仅调用原验证一次。
- Input/output and state mapping: 实际authenticate前后monotonic时间→jwt.verify.duration；真实JWK get分支→HIT/MISS/EXPIRED；managerMap仅管理issuer。
- Error and edge behavior: 尚未进行密码学验证的拒绝不计Timer；成功/失败都计，finally保留原异常；metric失败受限日志；unknown kid既有single-flight不改。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
// 同Step签名编译前置；不代表GREEN，暂不启用生产Bean或独立提交。
原VerifiedJwtBO authenticate(String compactToken)及2/3参构造保持；Lombok factory可追加固定Core recorder，新的观察overload仅调用原验证一次。
// observe/requireWritableFields/appendStrict仅委托原验证/PDP/append；metric record暂不发信号，配置只声明具名factory。
// 后续测试必须因缺失字段WRITE/strict durability/usage/metrics行为失败，再回本文件按完整pseudocode实现。
```

- Verification contribution: 本Step `MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest`；只满足编译，不能代替下一RED验证。
- After this file: 签名可编译但行为尚缺，下一测试预期RED；不提交、不注册。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java`

- Purpose: 同Step编译前置：原流程一次verify；将真实可信事实与stage计时交观察者；仅建立兼容签名/原委托，暂不注册/启用，后续同路径完成GREEN。
- Symbols: 原verifyMultiIssuer的metric overload及callback编译契约；IdpJwtVerifier。
- Repository evidence: 当前路径存在，已有verifyAny/verifyMultiIssuer观察overload；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: Step5 Core port/原VerifiedJwtBO/manager；旧User/Service wrappers；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 当前测试所需契约尚不存在，先建立仅原委托的可编译入口；后续立即真实RED。
- Contract/signature changes: 仅增加原验证与manager间传递Core recorder的overload/现有BO capture签名；不引用Step6 Proof/ObservedVerificationComponent。
- Input/output and state mapping: verifyMultiIssuer原authenticate→header/common/aud/type/scope/resource→revocation→Valid；回调只捕获本次真实验签BO；旧入口默认noop。
- Error and edge behavior: 缺可信capture或完整Valid不得生成proof；Invalid/Expired/Unavailable局部capture全部丢弃；configured managed observer只包最外层一次，防verifyUser→verifyAny重记。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
// 同Step签名编译前置；不代表GREEN，暂不启用生产Bean或独立提交。
仅增加原验证与manager间传递Core recorder的overload/现有BO capture签名；不引用Step6 Proof/ObservedVerificationComponent。
// observe/requireWritableFields/appendStrict仅委托原验证/PDP/append；metric record暂不发信号，配置只声明具名factory。
// 后续测试必须因缺失字段WRITE/strict durability/usage/metrics行为失败，再回本文件按完整pseudocode实现。
```

- Verification contribution: 本Step `MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest`；只满足编译，不能代替下一RED验证。
- After this file: 签名可编译但行为尚缺，下一测试预期RED；不提交、不注册。

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2Client.java`

- Purpose: 同Step编译前置：真实SERVICE缓存/重获计时，原cache key/single-flight/factory不变；仅建立兼容签名/原委托，暂不注册/启用，后续同路径完成GREEN。
- Symbols: authorize/renew观察overload；IdpServiceOAuth2Client。
- Repository evidence: 当前路径存在，已有authorize/renew观察overload；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 原OAuth2AuthorizedClientManager/Clock/本地cache；自有SERVICE调用与旧factory消费者；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 当前测试所需契约尚不存在，先建立仅原委托的可编译入口；后续立即真实RED。
- Contract/signature changes: authorize/renew观察overload与factory兼容；保持原授权key与single-flight。
- Input/output and state mapping: 实际cache lookup HIT/MISS/EXPIRED→counter，只有真正申请/临期重获→acquire duration；CLIENT_CREDENTIALS不虚构RT刷新。
- Error and edge behavior: 并发同key仍单飞；tenant/resource/scope/client/version维度不改；observer失败不丢token/不扩大scope；过期token不得复用。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
// 同Step签名编译前置；不代表GREEN，暂不启用生产Bean或独立提交。
authorize/renew观察overload与factory兼容；保持原授权key与single-flight。
// observe/requireWritableFields/appendStrict仅委托原验证/PDP/append；metric record暂不发信号，配置只声明具名factory。
// 后续测试必须因缺失字段WRITE/strict durability/usage/metrics行为失败，再回本文件按完整pseudocode实现。
```

- Verification contribution: 本Step `MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest`；只满足编译，不能代替下一RED验证。
- After this file: 签名可编译但行为尚缺，下一测试预期RED；不提交、不注册。

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/MultiIssuerResourceServerTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: MultiIssuerResourceServerTest；crypto_and_jwks_real_observation。
- Repository evidence: 当前路径存在，已有MultiIssuerResourceServerTest；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 既有MultiIssuerResourceServerTest actual RSA/JWKS cache +Clock revocation fixtures；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 新增/扩充crypto_and_jwks_real_observation方法；TEST-008/022/023/025受控行为断言，默认不启PG/Redis/Testcontainers。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: callback原验证一次、真实签名success/fail样本，错issuer/aud/tenant拒绝；JWK HIT/MISS/EXPIRED确实来自get；delay Redis不增密码学Timer
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void crypto_and_jwks_real_observation() {
    arrange 复用既有MultiIssuerResourceServerTest actual RSA/JWKS cache +Clock revocation fixtures，不启动应用或外部基础设施;
    act 调用该Step实际生产入口，不以手工调用record()/mock整段授权替代真实分支;
    assert callback原验证一次、真实签名success/fail样本，错issuer/aud/tenant拒绝；JWK HIT/MISS/EXPIRED确实来自get；delay Redis不增密码学Timer;
    verify 所有拒绝/不可用分支businessSpy为零且敏感值不进入event/labels/JSON;
}
```

- Verification contribution: 本Step `MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest`；callback原验证一次、真实签名success/fail样本，错issuer/aud/tenant拒绝；JWK HIT/MISS/EXPIRED确实来自get；delay Redis不增密码学Timer 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 7 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2ClientTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: IdpServiceOAuth2ClientTest；service_cache_real_observation。
- Repository evidence: 当前路径存在，已有IdpServiceOAuth2ClientTest；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 现有IdpServiceOAuth2ClientTest manager spy+Clock/cache/single-flight；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 新增/扩充service_cache_real_observation方法；TEST-023/025受控行为断言，默认不启PG/Redis/Testcontainers。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: 热key HIT、MISS/EXPIRED、临期重获timer仅一次；并发单飞/key隔离原样；无refresh_token概念、metric失败token不变
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void service_cache_real_observation() {
    arrange 复用现有IdpServiceOAuth2ClientTest manager spy+Clock/cache/single-flight，不启动应用或外部基础设施;
    act 调用该Step实际生产入口，不以手工调用record()/mock整段授权替代真实分支;
    assert 热key HIT、MISS/EXPIRED、临期重获timer仅一次；并发单飞/key隔离原样；无refresh_token概念、metric失败token不变;
    verify 所有拒绝/不可用分支businessSpy为零且敏感值不进入event/labels/JSON;
}
```

- Verification contribution: 本Step `MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest`；热key HIT、MISS/EXPIRED、临期重获timer仅一次；并发单飞/key隔离原样；无refresh_token概念、metric失败token不变 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 8 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java`

- Purpose: 真实JWT/JWK分支观察，保留2/3参构造和旧resolver接口（Edge调用保持）；本次只完成本Step所属切片。
- Symbols: authenticate(String compactToken) / manager builder / BoundedJwkSetCache；TenantIssuerAuthenticationManagerComponent。
- Repository evidence: 当前路径存在，已有authenticate观察overload/BoundedJwkSetCache；保留相邻注解/工厂/原异常风格。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 原TrustedIssuerRegistry/IdpStarterProperties/Clock；Core recorder固定绑定cache与Nimbus processor；Edge旧构造noop兼容；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 原VerifiedJwtBO authenticate(String compactToken)及2/3参构造保持；Lombok factory可追加固定Core recorder，新的观察overload仅调用原验证一次。
- Input/output and state mapping: 实际authenticate前后monotonic时间→jwt.verify.duration；真实JWK get分支→HIT/MISS/EXPIRED；managerMap仅管理issuer。
- Error and edge behavior: 尚未进行密码学验证的拒绝不计Timer；成功/失败都计，finally保留原异常；metric失败受限日志；unknown kid既有single-flight不改。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
manager(trusted)构建原NimbusJwtDecoder.withJwkSetUri(...).jwsAlgorithm(RS256).validateType(false).restOperations(原).cache(原);
通过已有jwtProcessorCustomizer：originalFactory=processor.getJWSVerifierFactory(); processor.setJWSVerifierFactory(Decorator originalFactory);
Decorator createJWSVerifier(header,key)→包装原JWSVerifier；仅verify(JWSHeader,byte[],Base64URL)前后nano计时，supportedJWSAlgorithms/getJCAContext均委托原对象；success/false/异常分别有限结果，finally安全record;
// key selector/JWKS获取、header解析、claim guards、Redis/audit都在密码学verify计时之外；未调用真正verify则无Timer样本。
BoundedJwkSetCache.get/get(loader)真实Clock分支记录HIT/MISS/EXPIRED；不记managerMap命中；旧ctor与resolver保持。
// 新factory依Lombok参数配一次recorder，不在请求中修改缓存绑定或引入ThreadLocal。
```

- Verification contribution: 本Step `MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest`；尚未进行密码学验证的拒绝不计Timer；成功/失败都计，finally保留原异常；metric失败受限日志；unknown kid既有single-flight不改。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 9 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java`

- Purpose: 原流程一次verify；将真实可信事实与stage计时交观察者；本次只完成本Step所属切片。
- Symbols: 原verifyMultiIssuer的metric overload及callback编译契约；IdpJwtVerifier。
- Repository evidence: 当前路径存在，已有verifyAny/verifyMultiIssuer观察overload；保留相邻注解/工厂/原异常风格。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: Step5 Core port/原VerifiedJwtBO/manager；旧User/Service wrappers；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 仅增加原验证与manager间传递Core recorder的overload/现有BO capture签名；不引用Step6 Proof/ObservedVerificationComponent。
- Input/output and state mapping: verifyMultiIssuer原authenticate→header/common/aud/type/scope/resource→revocation→Valid；回调只捕获本次真实验签BO；旧入口默认noop。
- Error and edge behavior: 缺可信capture或完整Valid不得生成proof；Invalid/Expired/Unavailable局部capture全部丢弃；configured managed observer只包最外层一次，防verifyUser→verifyAny重记。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
verifyMultiIssuer(token,type,recorder,capture) { verified=原manager.authenticate(token,recorder);
    原header/common/aud/type/scope/resource/revoke按顺序检查；capture仅来自真实VerifiedJwtBO；不再decode;
    return 原AccessTokenVerification结果；此阶段无proof/usage;
}
// 原createConfigured/verifyUser/Service/Any所有签名保持，无观察的旧入口走noop；Step6才组合managed usage。
// 编译前置先定义委托overload；真实RED后仅加入实际timing/cache观察。
```

- Verification contribution: 本Step `MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest`；缺可信capture或完整Valid不得生成proof；Invalid/Expired/Unavailable局部capture全部丢弃；configured managed observer只包最外层一次，防verifyUser→verifyAny重记。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 10 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2Client.java`

- Purpose: 真实SERVICE缓存/重获计时，原cache key/single-flight/factory不变；本次只完成本Step所属切片。
- Symbols: authorize/renew观察overload；IdpServiceOAuth2Client。
- Repository evidence: 当前路径存在，已有authorize/renew观察overload；保留相邻注解/工厂/原异常风格。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 原OAuth2AuthorizedClientManager/Clock/本地cache；自有SERVICE调用与旧factory消费者；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: authorize/renew观察overload与factory兼容；保持原授权key与single-flight。
- Input/output and state mapping: 实际cache lookup HIT/MISS/EXPIRED→counter，只有真正申请/临期重获→acquire duration；CLIENT_CREDENTIALS不虚构RT刷新。
- Error and edge behavior: 并发同key仍单飞；tenant/resource/scope/client/version维度不改；observer失败不丢token/不扩大scope；过期token不得复用。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
authorize(原参数,recorder) {
    cached = 原精确key缓存; record原HIT/MISS/EXPIRED分支;
    return 原singleFlight(key, () -> { started=System.nanoTime(); try { return 原manager.authorize(request); } finally { record acquire Duration; } });
}
// 新overload只观察；旧factory/authorize走noop；WebClient仍携带该原token。
```

- Verification contribution: 本Step `MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest`；并发同key仍单飞；tenant/resource/scope/client/version维度不改；observer失败不丢token/不扩大scope；过期token不得复用。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 11 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java`

- Purpose: 显式named/qualified装配，disabled不增加依赖；自身开启缺能力失败；本次只完成本Step所属切片。
- Symbols: manager / configured verifier / SERVICE client的Core recorder兼容factory；IdpStarterAutoConfiguration。
- Repository evidence: 当前路径存在，已有观察/producer组件Beans；保留相邻注解/工厂/原异常风格。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 原manager/verifier/client/具名Clock与properties；ObjectProvider<IdpSecurityMetricsRecorder>仅Core类型；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 仅把Core port传到已完成的manager/cache/client观察factory；没有metrics host时noop；新usage类型到Step6才引用。
- Input/output and state mapping: managed IdpJwtVerifier createConfigured观察组合→旧final USER/SERVICE包装器及HTTP combined→RPC；disabled仍原构造。
- Error and edge behavior: 开启要求真实producer/converter/Validator/host recorder；缺能力失败，不能ConditionalOnBean静默跳过安全能力；避免shared spy造成循环；Servlet filter仅在chain内启用一次。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
现有manager factory新增qualified Core recorder provider，getIfAvailable只返回现有host port或noop；不创建MeterRegistry;
使用Lombok新增兼容factory参数在initialize之前将该固定port传给cache/Nimbus verifier hooks；旧2/3参constructor仍供Edge调用;
原configured IdpJwtVerifier与SERVICE client观察overload接同一port，旧typed USER/SERVICE Bean类型保持;
// 此时无usage properties/ObservedVerificationComponent/proof，不引入未建立类型；Step7真实host port后再验证enabled缺能力。
```

- Verification contribution: 本Step `MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest`；开启要求真实producer/converter/Validator/host recorder；缺能力失败，不能ConditionalOnBean静默跳过安全能力；避免shared spy造成循环；Servlet filter仅在chain内启用一次。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`（Maven）；Step11 npm用下列完整 --prefix，无服务进程。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest -Dsurefire.failIfNoSpecifiedTests=false test`；该Step `git diff --check` 与精确commit path审查。
- Expected result: RED必须是上述缺失行为断言；GREEN exit0、目标命名tests实际执行且0 failures/errors/非预期skip；无源schema、Bean注入/敏感值或旧机制退化。Step11普通解析不得走临时alias。
- Failure returns to: 本Step对应assert的生产File；schema/DI到对应载体/factory，receipt状态到Step3/4，原合同退化停止当前Step；artifact缺失停止Step11等待可信输入；若需加库/改SQL/外platform或改Spec语义，回用户/Spec。
- Completion criteria: Core纯port与真实密码学/JWK/SERVICE信号可验证；旧构造/factory/key/单飞兼容；host接线尚未启用；保存实际RED/GREEN命令、测试report/准确数量、适用MC证据及当前HEAD，没有业务未完成签名/placeholder或外部runtime假成功。
- Rollback: 未部署时只还原本Step相对前置HEAD的精确diff，保留之前Step及未归属改动；已产生审计/spool时禁止删帧/回旧consumer，按§9停止入口并forward-fix。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/observability/SecurityMetricObservationBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/observability/IdpSecurityMetricsRecorder.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2Client.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/MultiIssuerResourceServerTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2ClientTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java`.
- Commit: `feat(shoubing): observe real token verification and caches`；本轮只规划，不执行。


### Step 6 — 为 HTTP 与 RPC 验证结果接上严格使用审计

- Requirements: REQ-001, REQ-002, REQ-004, REQ-005, REQ-006, REQ-009, REQ-014, REQ-015, REQ-016, REQ-018
- Dependencies: Step 3 已验证并提交, Step 4 已验证并提交, Step 5 已验证并提交
- Baseline state: 前置Step合同已GREEN并提交；本Step缺口：编译前置observer/filter只委托原验证/chain，真实HTTP/RPC调用仍无usage帧/MQ，handoff-before-return与proof恢复断言失败
- Observable outcome: 为 HTTP 与 RPC 验证结果接上严格使用审计；以本Step命名真实行为断言证明，错误分支和负作用同时核对。
- End state: configured原verifier覆盖USER/SERVICE/combined与RPC，失败匿名fallback，Valid携带secret-free proof；自身开关仍待配置启用。
- Test-first gate: Required — 编译前置observer/filter只委托原验证/chain，真实HTTP/RPC调用仍无usage帧/MQ，handoff-before-return与proof恢复断言失败；按File顺序签名编译前置后才运行RED，编译/fixture/环境失败不计。
- Manual Checks: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001；按每File Standards impact及本Step验证分别闭合。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/TokenAuditProofBO.java`

- Purpose: 可信issuer/resource及脱敏refs，允许跨SecurityContext/gRPC当前调用保留；不含JWT；本次只完成本Step所属切片。
- Symbols: immutable verified proof；TokenAuditProofBO。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，TokenUsageAuditEventConverter/AccessTokenVerification.Valid；Idp/RBAC Authentication与gRPC Context是现成邻近能力。
- Dependencies and consumers: TokenUsageAuditEventConverter/AccessTokenVerification.Valid；Idp/RBAC Authentication与gRPC Context；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: immutable record TokenAuditProofBO(URI issuer,URI resource,String subjectRef,String clientRef,String tokenRef)。
- Input/output and state mapping: generated mapper从完整可信JWT投影精确issuer与配置resource及受控refs；不含principal明文/jti/raw JWT。
- Error and edge behavior: required URI来自可信HTTPS配置；nullable refs仅hex64；compact constructor仅值不变量；@Valid/default；不当作授权凭据。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001：仅真正immutable record保留，集合防御复制/JsonIgnore proof；原构造和wire不变。 无业务DI；Date禁用，JSON采用Boot Jackson。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：上述精确角色、约束/建模与UTC Duration/Instant；URI及hash不可变；不另建模型/新层。
- Implementation pseudocode:

```java
record TokenAuditProofBO(@NotNull URI issuer,@NotNull URI resource,
        @Pattern(regexp="[0-9a-f]{64}") String subjectRef, String clientRef, String tokenRef) {
    // compact ctor保持URI不可变及refs受限；真实Value Object，豁免可变class注解
}
// proof只用于当前调用details/gRPC，不进入JWT、公开Valid JSON或日志。
```

- Verification contribution: 本Step `IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest`；required URI来自可信HTTPS配置；nullable refs仅hex64；compact constructor仅值不变量；@Valid/default；不当作授权凭据。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/AccessTokenVerification.java`

- Purpose: 保留原一参/两参构造/字段；新proof为JsonIgnore immutable metadata，legacy/disabled可为null；本次只完成本Step所属切片。
- Symbols: Valid可选proof；AccessTokenVerification。
- Repository evidence: 当前路径存在，已有Valid可选proof；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: IdpJwtVerifier/旧USER SERVICE wrapper/HTTP/RPC callers；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: Valid增加@JsonIgnore TokenAuditProofBO proof，保留Valid(principal)与Valid(principal,scopes)构造。
- Input/output and state mapping: 旧调用proof=null；observed Valid有immutable proof；principal/scopes排序与≤64约束不变。
- Error and edge behavior: 不改变Expired/Invalid/Unavailable、普通JSON字段；cast不得丢proof；敏感raw token不新增。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001：仅真正immutable record保留，集合防御复制/JsonIgnore proof；原构造和wire不变。 无业务DI；Date禁用，JSON采用Boot Jackson。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：上述精确角色、约束/建模与UTC Duration/Instant；URI及hash不可变；不另建模型/新层。
- Implementation pseudocode:

```java
record Valid<T extends IdpPrincipal>(T principal,Set<String> scopes,@JsonIgnore TokenAuditProofBO proof) {
    // 保留现有scopes防御复制、排序与格式上限
    Valid(T principal) { this(principal,Set.of(),null); }
    Valid(T principal,Set<String> scopes) { this(principal,scopes,null); }
}
assert Jackson.serialize(observedValid)只有原principal/scopes;
```

- Verification contribution: 本Step `IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest`；不改变Expired/Invalid/Unavailable、普通JSON字段；cast不得丢proof；敏感raw token不新增。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/TokenAuditContextBO.java`

- Purpose: 经验证BO+server-owned fallback/resource/result/clock，禁止JSON输出raw JWT；本次只完成本Step所属切片。
- Symbols: request context；TokenAuditContextBO。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，观察component/原VerifiedJwtBO/TokenUsageAuditEventConverter是现成邻近能力。
- Dependencies and consumers: 观察component/原VerifiedJwtBO/TokenUsageAuditEventConverter；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: class TokenAuditContextBO：proof、verifiedJwt、eventId、fallbackIssuer、resource、result、reasonCode、occurredAt、traceId。
- Input/output and state mapping: server-owned上下文→11字段Event；仅完整Valid提供proof，失败三refs null；身份不从header/caller声明。
- Error and edge behavior: verifiedJwt @JsonIgnore/@ToString.Exclude，proof @Valid；事件/trace正则上限；URI/Instant required；raw验证BO只在同步请求内持有。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001：普通class必须@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder；根class不callSuper；字段annotations/default/groups见上述contract。 无业务DI；Date禁用，JSON采用Boot Jackson。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：上述精确角色、约束/建模与UTC Duration/Instant；URI及hash不可变；不另建模型/新层。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder
class TokenAuditContextBO { @Valid TokenAuditProofBO proof; @JsonIgnore @ToString.Exclude VerifiedJwtBO verifiedJwt;
    @NotBlank @Size(max=64) String eventId; @NotNull URI fallbackIssuer; @NotNull URI resource;
    @NotNull Instant occurredAt; /* result/reasonCode/traceId按Spec §10.3注解 */ }
// 默认组检查；结束后清验证引用，不把此BO作为外部JSON/日志payload。
```

- Verification contribution: 本Step `IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest`；verifiedJwt @JsonIgnore/@ToString.Exclude，proof @Valid；事件/trace正则上限；URI/Instant required；raw验证BO只在同步请求内持有。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/converter/TokenUsageAuditEventConverter.java`

- Purpose: MapStruct+BaseForwardConverter投影到原11字段Event；本次只完成本Step所属切片。
- Symbols: toTarget / toProof(VerifiedJwtBO, contexts)；TokenUsageAuditEventConverter。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，Common BaseForwardConverter/原MapStruct processor/TokenUsageAuditComponent.reference；managed observer/UserInfo是现成邻近能力。
- Dependencies and consumers: Common BaseForwardConverter/原MapStruct processor/TokenUsageAuditComponent.reference；managed observer/UserInfo；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: @Mapper(componentModel="spring") extends BaseForwardConverter<TokenAuditContextBO,TokenUsageAuditEvent>；增加toProof真实投影。
- Input/output and state mapping: ignoreByDefault映射eventId、schemaVersion=1、可信issuer或fallback、resource、refs、result/reasonCode、occurredAt/traceId；proof→Event复用refs。
- Error and edge behavior: tokenRef由producer受控HMAC函数映射issuer/jti；subject/client哈希复用既有策略；capture失败refs全null；禁止setter/JSON往返映射。
- Standards impact: MC-CONVERT-001, MC-BEAN-001, MC-VALID-001, MC-MODEL-001：@Mapper(componentModel="spring")+Common BaseForwardConverter、ignoreByDefault映射；generated实现具名，真实生成代码测试，接口无业务Slf4j/构造注入。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 11：generated mapper及字段明确映射、目标schema手交接验证；不可手工copy/JSON往返替代。
- Implementation pseudocode:

```java
@Mapper(componentModel="spring") interface TokenUsageAuditEventConverter extends BaseForwardConverter<TokenAuditContextBO,TokenUsageAuditEvent> {
    @BeanMapping(ignoreByDefault=true) TokenUsageAuditEvent toTarget(TokenAuditContextBO source);
    // eventId←source.eventId；schemaVersion=1；issuer←proof.issuer或fallbackIssuer；resource←配置resource.toASCIIString；subject/client/token refs←proof nullable；result/reasonCode/occurredAt/traceId逐一同名。
    TokenAuditProofBO toProof(VerifiedJwtBO verified,@Context URI resource,@Context TokenUsageAuditComponent references);
    // issuer←verified.trustedIssuer.issuer；subjectRef←SHA256(verified.subject)；clientRef←SHA256(已可信jwt.client_id)（没有则null）；tokenRef←reference(issuer,verified.tokenId)。
}
// generated tokenUsageAuditEventConverterImpl；安全reference函数复用producer挂载key，Default/cascade校验、null失败fallback、无set/get/JSON往返。
```

- Verification contribution: 本Step `IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest`；tokenRef由producer受控HMAC函数映射issuer/jti；subject/client哈希复用既有策略；capture失败refs全null；禁止setter/JSON往返映射。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/OAuthUsageObservationProperties.java`

- Purpose: opt-in与接收方匿名失败审计authority；本次只完成本Step所属切片。
- Symbols: enabled/fallbackIssuer；OAuthUsageObservationProperties。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，Boot binder/IdpStarterAutoConfiguration/两host ObservationConfiguration是现成邻近能力。
- Dependencies and consumers: Boot binder/IdpStarterAutoConfiguration/两host ObservationConfiguration；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: @ConfigurationProperties("egon.cola.platform.tianquan.shoubing.usage-observation") class OAuthUsageObservationProperties。
- Input/output and state mapping: enabled=false、fallbackIssuer nullable仅disabled、allowLocalPlaintext=false；开启由host校验trusted receiver。
- Error and edge behavior: typed URI @Valid/default；enabled缺依赖或fallback不允许noop；local例外须profile+env+显式开关，issuer仍HTTPS。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001, MC-TIME-001：普通class必须@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder；根class不callSuper；字段annotations/default/groups见上述contract。 无业务DI；Date禁用，JSON采用Boot Jackson。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：上述精确角色、约束/建模与UTC Duration/Instant；URI及hash不可变；不另建模型/新层。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder @Validated
@ConfigurationProperties("egon.cola.platform.tianquan.shoubing.usage-observation")
class OAuthUsageObservationProperties { boolean enabled=false; URI fallbackIssuer; boolean allowLocalPlaintext=false; }
// enabled条件schema检查与host可信registry/TLS检查分开；所有profile同键，不打印secret。
```

- Verification contribution: 本Step `IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest`；typed URI @Valid/default；enabled缺依赖或fallback不允许noop；local例外须profile+env+显式开关，issuer仍HTTPS。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`

- Purpose: 补齐Spec已规定的producer-owned脱敏reference入口；原Plan误把该符号认作既有API。只复用现有mac/key，不修改Step3/4行为。
- Symbols: `reference(URI issuer,String tokenId)`。
- Repository evidence: 当前只有private `mac(byte[])`及`verifySignature`；没有可调用reference；新Mapper不能复用私有方法，也不能另读密钥或反射调用。
- Dependencies and consumers: 原挂载key/JDK Mac；本Step TokenUsageAuditEventConverter.toProof；其他平台与模板不变。
- Why now: Mapper进入真实编译前必须提供已批准Spec的技术helper；最新用户AGENTS必要依赖文件修改授权，唯一文件已在全局66路径库存中。
- Contract/signature changes: 新public技术方法`String reference(@NotNull URI issuer,@NotBlank @Pattern(regexp="[1-9][0-9]{0,18}") String tokenId)`；原公开接口/构造全部保留。
- Input/output and state mapping: 仅合法完整HTTPS issuer与已验证jti；返回原key的HMAC-SHA256(issuer.toASCIIString()+明确newline+tokenId)，hex64；不返回key或JWT。
- Error and edge behavior: URI无userinfo/query/fragment；tokenId形状不合法拒绝；key/I/O失败沿原OAUTH_AUDIT_HMAC_UNAVAILABLE；不记录输入。
- Standards impact: MC-VALID-001/UTIL/LOG/REUSE/SCOPE；原Validated/Slf4j/RequiredArgs/qualified依赖保持，native边界加原生约束，Common/MapStruct下游Default校验。
- Literal rule enforcement: Rule 1,2,4,5,6,9,10,11：现有组件角色、注解校验、JDK与原key owner，零新框架或消息字段。
- Implementation pseudocode:

```java
String reference(URI issuer,String tokenId) {
    validate canonical HTTPS URI and existing positive decimal jti shape;
    return HexFormat.of().formatHex(mac((issuer.toASCIIString()+"\n"+tokenId).getBytes(UTF_8)));
}
```

- Verification contribution: 本Step新IdpSecurityObservationFilterTest真实Mapper/producer验证golden HMAC、不同issuer/jti隔离、非法输入与raw零输出；沿本Stepexactfocused命令。
- After this file: 已批准helper可供Mapper；先真实RED再实现usage观察，不修改已提交前置Step。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpObservedVerificationComponent.java`

- Purpose: 同Step编译前置：包装自身managed USER/SERVICE verifier与RPC返回前审计；复用原验证，不新decode；仅建立兼容签名/原委托，暂不注册/启用，后续同路径完成GREEN。
- Symbols: observe；IdpObservedVerificationComponent。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，qualified tokenUsageAuditComponent/tokenUsageAuditEventConverterImpl/oauthUsageObservationProperties/idpSecurityMetricsRecorder/idpClock/defaultValidator；configured IdpJwtVerifier是现成邻近能力。
- Dependencies and consumers: qualified tokenUsageAuditComponent/tokenUsageAuditEventConverterImpl/oauthUsageObservationProperties/idpSecurityMetricsRecorder/idpClock/defaultValidator；configured IdpJwtVerifier；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 当前测试所需契约尚不存在，先建立仅原委托的可编译入口；后续立即真实RED。
- Contract/signature changes: <P extends IdpPrincipal> AccessTokenVerification<P> observe(Function<Consumer<VerifiedJwtBO>,AccessTokenVerification<P>> originalVerification)。
- Input/output and state mapping: 一次delegate→完整typed result及capture→proof/context→schema1 Event→appendStrict→typed返回；server生成eventId/Clock/trace。
- Error and edge behavior: 完整Valid却无capture在enabled失败closed；其他失败统一fallback与null refs；append失败Unavailable；metrics失败不改变原认证异常。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
// 同Step签名编译前置；不代表GREEN，暂不启用生产Bean或独立提交。
<P extends IdpPrincipal> AccessTokenVerification<P> observe(Function<Consumer<VerifiedJwtBO>,AccessTokenVerification<P>> originalVerification)。
// observe/requireWritableFields/appendStrict仅委托原验证/PDP/append；metric record暂不发信号，配置只声明具名factory。
// 后续测试必须因缺失字段WRITE/strict durability/usage/metrics行为失败，再回本文件按完整pseudocode实现。
```

- Verification contribution: 本Step `IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest`；只满足编译，不能代替下一RED验证。
- After this file: 签名可编译但行为尚缺，下一测试预期RED；不提交、不注册。

#### File 8 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpSecurityObservationFilter.java`

- Purpose: 同Step编译前置：使用审计/拒绝观察的真实Servlet执行点；只在自身enabled链接入；仅建立兼容签名/原委托，暂不注册/启用，后续同路径完成GREEN。
- Symbols: doFilterInternal；IdpSecurityObservationFilter。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，qualified observed verifier/producer/converter/properties/Clock；两Admin仅SecurityFilterChain注册，禁Servlet重复注册是现成邻近能力。
- Dependencies and consumers: qualified observed verifier/producer/converter/properties/Clock；两Admin仅SecurityFilterChain注册，禁Servlet重复注册；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 当前测试所需契约尚不存在，先建立仅原委托的可编译入口；后续立即真实RED。
- Contract/signature changes: IdpSecurityObservationFilter#doFilterInternal；Servlet独立授权阶段观察与响应前审计。
- Input/output and state mapping: 只受保护Bearer请求；读取当前可信proof和server trace；对授权拒绝投影DENIED，TOKEN_USE_VERIFIED由managed verifier拥有。
- Error and edge behavior: public/preflight/disabled跳过；PDP拒绝render前appendStrict一次；业务500不记JWT失败；渲染前审计失败503/业务零调用；finally清请求标记。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
// 同Step签名编译前置；不代表GREEN，暂不启用生产Bean或独立提交。
IdpSecurityObservationFilter#doFilterInternal；Servlet独立授权阶段观察与响应前审计。
// observe/requireWritableFields/appendStrict仅委托原验证/PDP/append；metric record暂不发信号，配置只声明具名factory。
// 后续测试必须因缺失字段WRITE/strict durability/usage/metrics行为失败，再回本文件按完整pseudocode实现。
```

- Verification contribution: 本Step `IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest`；只满足编译，不能代替下一RED验证。
- After this file: 签名可编译但行为尚缺，下一测试预期RED；不提交、不注册。

#### File 9 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpSecurityObservationFilterTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: IdpSecurityObservationFilterTest；observed_http_rpc_and_cleanup。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，真实filters/configured verifier/generated Mapper/既有RPC interceptor、固定RSA与Clock是现成邻近能力。
- Dependencies and consumers: 真实filters/configured verifier/generated Mapper/既有RPC interceptor、固定RSA与Clock；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 新增/扩充observed_http_rpc_and_cleanup方法；TEST-005/006/008受控行为断言，默认不启PG/Redis/Testcontainers。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: USER/SERVICE仅一次verify、handoff先业务；invalid partial capture全null fallback；no proof failclosed；RPC callback proof/取消恢复；disabled/public/preflight不记；raw不入JSON
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void observed_http_rpc_and_cleanup() {
    arrange 复用真实filters/configured verifier/generated Mapper/既有RPC interceptor、固定RSA与Clock，不启动应用或外部基础设施;
    act 调用该Step实际生产入口，不以手工调用record()/mock整段授权替代真实分支;
    assert USER/SERVICE仅一次verify、handoff先业务；invalid partial capture全null fallback；no proof failclosed；RPC callback proof/取消恢复；disabled/public/preflight不记；raw不入JSON;
    verify 所有拒绝/不可用分支businessSpy为零且敏感值不进入event/labels/JSON;
}
```

- Verification contribution: 本Step `IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest`；USER/SERVICE仅一次verify、handoff先业务；invalid partial capture全null fallback；no proof failclosed；RPC callback proof/取消恢复；disabled/public/preflight不记；raw不入JSON 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 10 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpObservedVerificationComponent.java`

- Purpose: 包装自身managed USER/SERVICE verifier与RPC返回前审计；复用原验证，不新decode；本次只完成本Step所属切片。
- Symbols: observe；IdpObservedVerificationComponent。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，qualified tokenUsageAuditComponent/tokenUsageAuditEventConverterImpl/oauthUsageObservationProperties/idpSecurityMetricsRecorder/idpClock/defaultValidator；configured IdpJwtVerifier是现成邻近能力。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: qualified tokenUsageAuditComponent/tokenUsageAuditEventConverterImpl/oauthUsageObservationProperties/idpSecurityMetricsRecorder/idpClock/defaultValidator；configured IdpJwtVerifier；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: <P extends IdpPrincipal> AccessTokenVerification<P> observe(Function<Consumer<VerifiedJwtBO>,AccessTokenVerification<P>> originalVerification)。
- Input/output and state mapping: 一次delegate→完整typed result及capture→proof/context→schema1 Event→appendStrict→typed返回；server生成eventId/Clock/trace。
- Error and edge behavior: 完整Valid却无capture在enabled失败closed；其他失败统一fallback与null refs；append失败Unavailable；metrics失败不改变原认证异常。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
observe(originalVerification) {
    capture = 本次局部引用; result = originalVerification.apply(capture::accept);
    proof = result instanceof Valid && capture完整可信 ? converter.toProof(capture,配置resource,key上下文) : null;
    context = server context(固定eventId,proof或fallback,阶段结果,clock.instant(),合法trace); new ValidationUtils(defaultValidator).validate(context);
    producer.appendStrict(converter.toTarget(context)); // 在return之前；Event仍11字段
    return validWithProofOrOriginal(result,proof); // no raw token/no second decode
}
```

- Verification contribution: 本Step `IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest`；完整Valid却无capture在enabled失败closed；其他失败统一fallback与null refs；append失败Unavailable；metrics失败不改变原认证异常。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 11 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpSecurityObservationFilter.java`

- Purpose: 使用审计/拒绝观察的真实Servlet执行点；只在自身enabled链接入；本次只完成本Step所属切片。
- Symbols: doFilterInternal；IdpSecurityObservationFilter。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，qualified observed verifier/producer/converter/properties/Clock；两Admin仅SecurityFilterChain注册，禁Servlet重复注册是现成邻近能力。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: qualified observed verifier/producer/converter/properties/Clock；两Admin仅SecurityFilterChain注册，禁Servlet重复注册；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: IdpSecurityObservationFilter#doFilterInternal；Servlet独立授权阶段观察与响应前审计。
- Input/output and state mapping: 只受保护Bearer请求；读取当前可信proof和server trace；对授权拒绝投影DENIED，TOKEN_USE_VERIFIED由managed verifier拥有。
- Error and edge behavior: public/preflight/disabled跳过；PDP拒绝render前appendStrict一次；业务500不记JWT失败；渲染前审计失败503/业务零调用；finally清请求标记。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
doFilterInternal(request,response,chain) {
    disabled/publicHealth/nativePreflight直接原chain；受保护路径先建立request属性中的decision-stage记录容器与合法server trace;
    为原Idp/RBAC renderer提供pre-render callback，拒绝必须strict handoff先于写response，audit失败503;
    验证usage归managed observer；后续PDP自己的拒绝事实由Step9 Decorator拥有，renderer只补尚未记录的同一决策，不在response后审计;
    try { chain.doFilter(request,response); } finally { 清request属性；原业务500不算JWT失败; }
}
// RPC以现有gRPC Context携带proof/受控阶段标记，无额外ThreadLocal；非Token本地PDP不造usage。
```

- Verification contribution: 本Step `IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest`；public/preflight/disabled跳过；PDP拒绝render前appendStrict一次；业务500不记JWT失败；渲染前审计失败503/业务零调用；finally清请求标记。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 12 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java`

- Purpose: 原流程一次verify；将真实可信事实与stage计时交观察者；本次只完成本Step所属切片。
- Symbols: verifyAny/verifyMultiIssuer观察overload；IdpJwtVerifier。
- Repository evidence: 当前路径存在，已有verifyAny/verifyMultiIssuer观察overload；保留相邻注解/工厂/原异常风格。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 原manager/revocation/VerifiedPrincipalConverter；USER/SERVICE final包装类→此configured verifier；HTTP与RPC；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 保留verifyUser/verifyService/verifyAny和createConfigured旧签名；增加Consumer<VerifiedJwtBO>/metrics观察overload及配置factory组合。
- Input/output and state mapping: verifyMultiIssuer原authenticate→header/common/aud/type/scope/resource→revocation→Valid；回调只捕获本次真实验签BO；旧入口默认noop。
- Error and edge behavior: 缺可信capture或完整Valid不得生成proof；Invalid/Expired/Unavailable局部capture全部丢弃；configured managed observer只包最外层一次，防verifyUser→verifyAny重记。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
verifyAny(token, capture, recorder) {
    original = verifyMultiIssuer(token, expectedType, capture, recorder); // 仍只执行一次原验证
    return original; // 不在此重新decode、不以tenant猜issuer
}
// enabled factory绑定IdpObservedVerificationComponent：public verifyUser/Service/Any最外层observe(() -> 原内部验证)，内部overload不再observe。
// 用Lombok create扩展不可变factory参数；旧factory/构造传noop/null；castVerification保留Valid.proof。
```

- Verification contribution: 本Step `IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest`；缺可信capture或完整Valid不得生成proof；Invalid/Expired/Unavailable局部capture全部丢弃；configured managed observer只包最外层一次，防verifyUser→verifyAny重记。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 13 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpBearerAuthenticationFilter.java`

- Purpose: 原身份安装/清理保留；按opt-in把本请求可信事实交usage filter；本次只完成本Step所属切片。
- Symbols: create/请求观察回调；IdpBearerAuthenticationFilter。
- Repository evidence: 当前路径存在，已有create/请求观察回调；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: IdpJwtVerifier/AccessTokenVerification/endpointPolicy/mapper；自身Security链与JS bridge；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: IdpBearerAuthenticationFilter#create兼容；observed Valid.proof→Authentication.details。
- Input/output and state mapping: 原typed结果→原身份安装；proof复制为secret-free details，回调限定在本次请求；tenant与SecurityContext按原finally恢复。
- Error and edge behavior: invalid/missing Bearer仍旧401，audit Unavailable503；原token不进入Spring credentials或日志；不在filter再次记录已经managed observer完成的验证事件。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
doFilterInternal(request,response,chain) {
    // missing/重复/非法Bearer header须在原401 renderer前按server fallback/null refs完成该决策strict审计，不能等待chain返回。
    verification=原configured verifier.verifyAny(token); // 有token的验证usage由managed observer拥有，filter不重复
    Valid时原Authentication.setDetails(valid.proof())，原身份/tenant安装；Invalid/Unavailable沿401/503原族并no-store;
    try { 原chain或原renderer; } finally { 原SecurityContext/tenant恢复并清本请求决策标记; }
}
```

- Verification contribution: 本Step `IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest`；invalid/missing Bearer仍旧401，audit Unavailable503；原token不进入Spring credentials或日志；不在filter再次记录已经managed observer完成的验证事件。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 14 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/rpc/IdpRpcSecurityContext.java`

- Purpose: 保留原with/run方法；当前gRPC Context持有secret-free proof并finally恢复，不新增ThreadLocal；本次只完成本Step所属切片。
- Symbols: 可选proof overload；IdpRpcSecurityContext。
- Repository evidence: 当前路径存在，已有可选proof overload；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: io.grpc.Context/原IdpAuthenticationToken；IdpRpcBearerServerInterceptor→业务PDP；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 既有with/runWithSpringSecurity保留；新增带TokenAuditProofBO的overload与currentProofOrNull。
- Input/output and state mapping: 现有gRPC Context新增可选proof Key；runWithSpringSecurity设置Authentication.details；raw token仍仅原relay Context。
- Error and edge behavior: 旧签名在未传proof时行为一致；callback finally恢复之前SecurityContext；不新增ThreadLocal，取消/异常都恢复。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
with(principal,token,proof) { return 原Context.withValues(PRINCIPAL,principal,TOKEN,token).withValue(PROOF,proof); }
runWithSpringSecurity(principal,proof,callback) {
    previous = SecurityContextHolder.getContext(); current = 原空context与IdpAuthenticationToken; current.authentication.setDetails(proof);
    try { SecurityContextHolder.setContext(current); callback.run(); } finally { SecurityContextHolder.setContext(previous); }
}
// 原overload委托当前Context proof或null，不把raw token放进Spring credentials。
```

- Verification contribution: 本Step `IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest`；旧签名在未传proof时行为一致；callback finally恢复之前SecurityContext；不新增ThreadLocal，取消/异常都恢复。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 15 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/rpc/IdpRpcBearerServerInterceptor.java`

- Purpose: 传递Valid.proof到现有RPC Context；本次只完成本Step所属切片。
- Symbols: interceptCall / listener callbacks；IdpRpcBearerServerInterceptor。
- Repository evidence: 当前路径存在，已有interceptCall / listener callbacks；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 现有final UserAccessTokenVerifier/Valid/IdpRpcSecurityContext；用户明确批准补齐本调用点，无外平台改动；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: interceptCall使用Valid.proof，带proof的with/runWithSpringSecurity overload；保留所有旧公开签名。
- Input/output and state mapping: verifier Valid→principal/proof+原token relay→现有gRPC Context→listener callbacks Spring details→PDP。
- Error and edge behavior: 原UNAUTHENTICATED/UNAVAILABLE映射、缺/重复header和长度守卫不变；所有callback finally恢复；不把token存credentials；原with(principal,token)仍可用。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
verification = verifier.verify(token); // 已configured managed observe，先strict handoff
if (verification instanceof Valid valid) {
    context = IdpRpcSecurityContext.with((IdentityPrincipal)valid.principal(),token,valid.proof());
    listener callbacks在context.run中调用runWithSpringSecurity(principal,valid.proof(),callback);
}
// onMessage/onHalfClose/onCancel/onComplete/onReady全部沿同一已verified proof；异常/取消恢复原context。
```

- Verification contribution: 本Step `IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest`；原UNAUTHENTICATED/UNAVAILABLE映射、缺/重复header和长度守卫不变；所有callback finally恢复；不把token存credentials；原with(principal,token)仍可用。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 16 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java`

- Purpose: 显式named/qualified装配，disabled不增加依赖；自身开启缺能力失败；本次只完成本Step所属切片。
- Symbols: 观察/producer组件Beans；IdpStarterAutoConfiguration。
- Repository evidence: 当前路径存在，已有观察/producer组件Beans；保留相邻注解/工厂/原异常风格。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 旧自动装配/qualified Jackson+Redisson+Clock/Common Validator；SS两typed ports、Servlet、RPC、两Admin；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 具名Props/producer/observed verifier/metrics port装配；原UserAccessTokenVerifier/ServiceAccessTokenVerifier类型与Bean名保持。
- Input/output and state mapping: managed IdpJwtVerifier createConfigured观察组合→旧final USER/SERVICE包装器及HTTP combined→RPC；disabled仍原构造。
- Error and edge behavior: 开启要求真实producer/converter/Validator/host recorder；缺能力失败，不能ConditionalOnBean静默跳过安全能力；避免shared spy造成循环；Servlet filter仅在chain内启用一次。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
@Bean(name="oauthUsageObservationProperties") bind并验证新Props;
@Bean(name="auditDeliveryProperties") bind新delivery，保留oauthAuditProperties;
@Bean(name="idpObservedVerificationComponent") enabled时注入具名producer/converter/recorder/Clock;
@Bean(name="idpJwtVerifier") configuredFactory绑定observer，旧最终USER/SERVICE wrappers继续委托；禁继承final verifier;
// 新filter的FilterRegistrationBean disabled；host实际chain显式接入；旧host metrics缺省noop且无strict审计。
```

- Verification contribution: 本Step `IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest`；开启要求真实producer/converter/Validator/host recorder；缺能力失败，不能ConditionalOnBean静默跳过安全能力；避免shared spy造成循环；Servlet filter仅在chain内启用一次。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 17 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfigurationTest.java`

- Purpose: 生产具名装配的现有fixture校正，仅维持原断言。
- Symbols: 三处`withBean("jacksonObjectMapper", ObjectMapper.class, ObjectMapper::new)`。
- Repository evidence: 旧fixture仅按类型提供Mapper；新具名factory与Boot bean名一致，校正前4个existing tests失败于missing qualifier。
- Dependencies and consumers: 原ApplicationContextRunner与Mapper；本Stepexactselector既已包含该test，无新工具链或runtime。
- Why now: final wiring regression揭示必要依赖fixture；最新用户AGENTS允许为正确性修复直接依赖文件，PC009记录。
- Contract/signature changes: 无；只test bean名。
- Input/output and state mapping: 原配置组合/断言不变；提供实际Boot稳定bean名。
- Error and edge behavior: 无放宽测试、无mock整段认证；其他平台配置不改。
- Standards impact: MC-BEAN-001/SCOPE/TEST；非生产Bean carrier，无业务Pojo新建。
- Literal rule enforcement: Rule 4,6,11；仅校正既有Boot Jackson注册，原结构不变。
- Implementation pseudocode:

```java
contextRunner = originalAutoConfigurationRunner
    .withBean("jacksonObjectMapper", ObjectMapper.class, ObjectMapper::new);
contextRunner.run(context -> assertThat(context).hasSingleBean(IdpBearerAuthenticationFilter.class));
resourceOnlyRunner = originalResourceOnlyRunner
    .withBean("jacksonObjectMapper", ObjectMapper.class, ObjectMapper::new);
serviceClientRunner = originalConfiguredClientRunner
    .withBean("jacksonObjectMapper", ObjectMapper.class, ObjectMapper::new);
retiredConfigurationRunner = originalRetiredConfigurationRunner
    .withBean("jacksonObjectMapper", ObjectMapper.class, ObjectMapper::new);
// Preserve each original configuration, expected failure and RPC override assertion; only use the Boot bean name.
```

- Verification contribution: 本Step existing8 auto config tests全通过（包括原RPC override与SERVICE配置）。
- After this file: 15-file Scope闭合，exact GREEN与门禁后commit。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`（Maven）；Step11 npm用下列完整 --prefix，无服务进程。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test`；该Step `git diff --check` 与精确commit path审查。
- Expected result: RED必须是上述缺失行为断言；GREEN exit0、目标命名tests实际执行且0 failures/errors/非预期skip；无源schema、Bean注入/敏感值或旧机制退化。Step11普通解析不得走临时alias。
- Failure returns to: 本Step对应assert的生产File；schema/DI到对应载体/factory，receipt状态到Step3/4，原合同退化停止当前Step；artifact缺失停止Step11等待可信输入；若需加库/改SQL/外platform或改Spec语义，回用户/Spec。
- Completion criteria: configured原verifier覆盖USER/SERVICE/combined与RPC，失败匿名fallback，Valid携带secret-free proof；自身开关仍待配置启用；保存实际RED/GREEN命令、测试report/准确数量、适用MC证据及当前HEAD，没有业务未完成签名/placeholder或外部runtime假成功。
- Rollback: 未部署时只还原本Step相对前置HEAD的精确diff，保留之前Step及未归属改动；已产生审计/spool时禁止删帧/回旧consumer，按§9停止入口并forward-fix。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/TokenAuditProofBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/AccessTokenVerification.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/TokenAuditContextBO.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/converter/TokenUsageAuditEventConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/OAuthUsageObservationProperties.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpObservedVerificationComponent.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpSecurityObservationFilter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpSecurityObservationFilterTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpBearerAuthenticationFilter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/rpc/IdpRpcSecurityContext.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/rpc/IdpRpcBearerServerInterceptor.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfigurationTest.java`.
- Commit: `fix(shoubing): audit protected HTTP and RPC token use`；本轮只规划，不执行。



- PC-009必要fixture修正：本Step装配参数依批准规则具名，旧test按类型提供ObjectMapper而非Boot具名jacksonObjectMapper。仅该既有测试文件给现有withBean加稳定名，并补入全局库存（66 unique paths）；行为/断言/工具链不变。MultiIssuerResourceServerTest已有稳定Bean名，不需修改。Step6 unique source paths=15；此修正依据最新用户AGENTS必要依赖文件修改授权。

### Step 7 — 把 Core 观测接到两个 Admin 的现有指标设施

- Requirements: REQ-001, REQ-009, REQ-010, REQ-011, REQ-014, REQ-015, REQ-018
- Dependencies: Step 5 已验证并提交, Step 6 已验证并提交
- Baseline state: 前置Step合同已GREEN并提交；本Step缺口：adapter编译前置为空观察/配置仍不启用；调用实际verifier/cache记录不到对应Timer/Counter，ContextRunner缺开启必需依赖断言失败
- Observable outcome: 把 Core 观测接到两个 Admin 的现有指标设施；以本Step命名真实行为断言证明，错误分支和负作用同时核对。
- End state: 两host固定receiver的真实registry adapter与具名Supplier可装配；缺能力不noop，SDK无Micrometer新依赖。
- Test-first gate: Required — adapter编译前置为空观察/配置仍不启用；调用实际verifier/cache记录不到对应Timer/Counter，ContextRunner缺开启必需依赖断言失败；按File顺序签名编译前置后才运行RED，编译/fixture/环境失败不计。
- Manual Checks: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001；按每File Standards impact及本Step验证分别闭合。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/pom.xml`

- Purpose: 已批准的编译能力前置，在行为测试前解除工具链缺口，不单独作为业务GREEN提交。
- Symbols: Lombok provided/optional 1.18.46；原Maven compiler配置。
- Repository evidence: 本轮Step2 preflight表给出当前POM/父依赖和缓存证据，用户明确批准。
- Dependencies and consumers: 当前Root Boot/BOM/Lombok与授柄既有MapStruct模式；本模块后续模型/组件编译消费者。
- Why now: 无此项，后续RED只能得到编译失败，不能验证批准的业务行为。
- Contract/signature changes: 仅上述编译API/provided/processor，不修改public Java/HTTP或SQL。
- Input/output and state mapping: POM现有版本→离线compile/generated目标；无DB/配置状态变化。
- Error and edge behavior: 缓存缺失停止，不下载；版本与已批准不符阻塞；原依赖保留。
- Standards impact: MC-DEP-001, MC-REUSE-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001：已有库/规范构造与mapper的最小支持。
- Literal rule enforcement: Rule 2, Rule 3, Rule 4, Rule 11：注解校验、完整Lombok/真实MapStruct生成，不以手写绕过。
- Implementation pseudocode:

```xml
<dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><version>${lombok.version}</version><scope>provided</scope><optional>true</optional></dependency>
<!-- Lombok provided/optional 1.18.46；继承Boot配置处理器与Lombok，JS追加现版本MapStruct/binding -->
<!-- 无新runtime依赖/升级/下载，真实generated Mapper或构造注入后验证 -->
```

- Verification contribution: 本Step原命名测试及离线编译；JS必须存在FieldWriteAuthorizationConverterImpl，JA验证生成Qualified构造。
- After this file: 编译前置可用，原行为未扩；继续本Step其余严格File顺序，RED/GREEN后一起提交。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityMetricsComponent.java`

- Purpose: 同Step编译前置：Core port→现有Micrometer，固定SHOUBING角色；仅建立兼容签名/原委托，暂不注册/启用，后续同路径完成GREEN。
- Symbols: record；TianquanSecurityMetricsComponent。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，现有Actuator/MeterRegistry/Common Validator/SC port；本host verifier/cache/PDP/producer是现成邻近能力。
- Dependencies and consumers: 现有Actuator/MeterRegistry/Common Validator/SC port；本host verifier/cache/PDP/producer；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 当前测试所需契约尚不存在，先建立仅原委托的可编译入口；后续立即真实RED。
- Contract/signature changes: TianquanSecurityMetricsComponent implements IdpSecurityMetricsRecorder；注入具名Supplier<MeterRegistry>及defaultValidator。
- Input/output and state mapping: host receiver固定SHOUBING；固定不可变method-reference策略表→Timer/Counter/Gauge；只允许有限result/reason category。
- Error and edge behavior: 多registry使用Boot primary/unique，绝不新建第二registry；恶意signal/负duration拒绝schema；运行metric异常只有限安全日志，不吞原验证/PDP异常。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
// 同Step签名编译前置；不代表GREEN，暂不启用生产Bean或独立提交。
TianquanSecurityMetricsComponent implements IdpSecurityMetricsRecorder；注入具名Supplier<MeterRegistry>及defaultValidator。
// observe/requireWritableFields/appendStrict仅委托原验证/PDP/append；metric record暂不发信号，配置只声明具名factory。
// 后续测试必须因缺失字段WRITE/strict durability/usage/metrics行为失败，再回本文件按完整pseudocode实现。
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,IdpStarterAutoConfigurationTest`；只满足编译，不能代替下一RED验证。
- After this file: 签名可编译但行为尚缺，下一测试预期RED；不提交、不注册。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityMetricsComponent.java`

- Purpose: 同Step编译前置：同Core port→本host Micrometer，固定JIANSHEN角色；仅建立兼容签名/原委托，暂不注册/启用，后续同路径完成GREEN。
- Symbols: record；TianquanSecurityMetricsComponent。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，现有Actuator/MeterRegistry/Common Validator/SC port；本host verifier/cache/PDP/producer是现成邻近能力。
- Dependencies and consumers: 现有Actuator/MeterRegistry/Common Validator/SC port；本host verifier/cache/PDP/producer；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 当前测试所需契约尚不存在，先建立仅原委托的可编译入口；后续立即真实RED。
- Contract/signature changes: TianquanSecurityMetricsComponent implements IdpSecurityMetricsRecorder；注入具名Supplier<MeterRegistry>及defaultValidator。
- Input/output and state mapping: host receiver固定JIANSHEN；固定不可变method-reference策略表→Timer/Counter/Gauge；只允许有限result/reason category。
- Error and edge behavior: 多registry使用Boot primary/unique，绝不新建第二registry；恶意signal/负duration拒绝schema；运行metric异常只有限安全日志，不吞原验证/PDP异常。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
// 同Step签名编译前置；不代表GREEN，暂不启用生产Bean或独立提交。
TianquanSecurityMetricsComponent implements IdpSecurityMetricsRecorder；注入具名Supplier<MeterRegistry>及defaultValidator。
// observe/requireWritableFields/appendStrict仅委托原验证/PDP/append；metric record暂不发信号，配置只声明具名factory。
// 后续测试必须因缺失字段WRITE/strict durability/usage/metrics行为失败，再回本文件按完整pseudocode实现。
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,IdpStarterAutoConfigurationTest`；只满足编译，不能代替下一RED验证。
- After this file: 签名可编译但行为尚缺，下一测试预期RED；不提交、不注册。

#### File 4 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityObservationConfiguration.java`

- Purpose: 同Step编译前置：复用Boot primary/unique registry，具名Supplier解决qualified依赖；仅建立兼容签名/原委托，暂不注册/启用，后续同路径完成GREEN。
- Symbols: idpMeterRegistrySupplier / idpSecurityMetricsRecorder factory / enabled dependency check；TianquanSecurityObservationConfiguration。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，Step5 Core port、Step6 observer/props、原Actuator/ObjectProvider/defaultValidator；自己的adapter是现成邻近能力。
- Dependencies and consumers: Step5 Core port、Step6 observer/props、原Actuator/ObjectProvider/defaultValidator；自己的adapter；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 当前测试所需契约尚不存在，先建立仅原委托的可编译入口；后续立即真实RED。
- Contract/signature changes: 只接现有Actuator与Core port并检查enabled具名必需能力；endpoint/TLS/profile实际启用在Step10。
- Input/output and state mapping: Spring ObjectProvider<MeterRegistry>解析primary/unique→JDK Supplier闭包；base/local部署属性→trusted HTTPS/TLS/readiness。
- Error and edge behavior: enabled缺registry/producer/key/spool/fallback/Validator启动失败；disabled不要求部署secret；local三重条件之外不接受plaintext；不存在第二MeterRegistry或按exporter名猜注入。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
// 同Step签名编译前置；不代表GREEN，暂不启用生产Bean或独立提交。
只接现有Actuator与Core port并检查enabled具名必需能力；endpoint/TLS/profile实际启用在Step10。
// observe/requireWritableFields/appendStrict仅委托原验证/PDP/append；metric record暂不发信号，配置只声明具名factory。
// 后续测试必须因缺失字段WRITE/strict durability/usage/metrics行为失败，再回本文件按完整pseudocode实现。
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,IdpStarterAutoConfigurationTest`；只满足编译，不能代替下一RED验证。
- After this file: 签名可编译但行为尚缺，下一测试预期RED；不提交、不注册。

#### File 5 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityObservationConfiguration.java`

- Purpose: 同Step编译前置：同安全schema/依赖标准，绝不创建第二MeterRegistry；仅建立兼容签名/原委托，暂不注册/启用，后续同路径完成GREEN。
- Symbols: idpMeterRegistrySupplier / idpSecurityMetricsRecorder factory / enabled dependency check；TianquanSecurityObservationConfiguration。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，Step5 Core port、Step6 observer/props、原Actuator/ObjectProvider/defaultValidator；自己的adapter是现成邻近能力。
- Dependencies and consumers: Step5 Core port、Step6 observer/props、原Actuator/ObjectProvider/defaultValidator；自己的adapter；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 当前测试所需契约尚不存在，先建立仅原委托的可编译入口；后续立即真实RED。
- Contract/signature changes: 只接现有Actuator与Core port并检查enabled具名必需能力；endpoint/TLS/profile实际启用在Step10。
- Input/output and state mapping: Spring ObjectProvider<MeterRegistry>解析primary/unique→JDK Supplier闭包；base/local部署属性→trusted HTTPS/TLS/readiness。
- Error and edge behavior: enabled缺registry/producer/key/spool/fallback/Validator启动失败；disabled不要求部署secret；local三重条件之外不接受plaintext；不存在第二MeterRegistry或按exporter名猜注入。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
// 同Step签名编译前置；不代表GREEN，暂不启用生产Bean或独立提交。
只接现有Actuator与Core port并检查enabled具名必需能力；endpoint/TLS/profile实际启用在Step10。
// observe/requireWritableFields/appendStrict仅委托原验证/PDP/append；metric record暂不发信号，配置只声明具名factory。
// 后续测试必须因缺失字段WRITE/strict durability/usage/metrics行为失败，再回本文件按完整pseudocode实现。
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,IdpStarterAutoConfigurationTest`；只满足编译，不能代替下一RED验证。
- After this file: 签名可编译但行为尚缺，下一测试预期RED；不提交、不注册。

#### File 6 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityMetricsContractTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: host_actual_metric_strategy_and_dependency_wiring；shoubing_real_metrics_and_wiring。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，Actuator SimpleMeterRegistry/原native SAS fixtures/真实MapStruct/ContextRunner是现成邻近能力。
- Dependencies and consumers: Actuator SimpleMeterRegistry/原native SAS fixtures/真实MapStruct/ContextRunner；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 只新增TEST022/023/024的JWT/cache/audit真实分支与固定host adapter、TEST026具名依赖检查；PDP在Step9、UserInfo在Step8、TLS/parity在Step10。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: 真实verifier/cache/producer发出Timer/Counter/Gauge、低基数/unsupported signal、metric抛错结果不变；registry primary/unique选择及缺enabled能力启动失败。
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void host_actual_metric_strategy_and_dependency_wiring() {
    registry=真实SimpleMeterRegistry，adapter=本host固定receiver；通过configured真实manager/SERVICEclient/producer触发分支;
    真实RSA成功/坏签名与受控JWK get HIT/MISS/EXPIRED；Clock推进触发SERVICE临期重获，并发single-flight仍一次;
    assert jwt.verify.duration只包原实际验签（不含Redis/audit delay），audit delivery/receipt age/spool bytes由真实状态产生;
    context runner使用qualified defaultValidator/primary registry且enabled缺producer/registry失败；metric抛错不改typed认证结果;
    // 不在本Step写尚未实现的UserInfo/PDP/TLS测试，后续同文件继续各自RED/GREEN。
}
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,IdpStarterAutoConfigurationTest`；真实verifier/cache/producer发出Timer/Counter/Gauge、低基数/unsupported signal、metric抛错结果不变；registry primary/unique选择及缺enabled能力启动失败。 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 7 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityMetricsContractTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: host_actual_metric_strategy_and_dependency_wiring；jianshen_real_metrics_and_wiring。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，Actuator SimpleMeterRegistry/既有AuthorizationService runtime fixtures/ContextRunner是现成邻近能力。
- Dependencies and consumers: Actuator SimpleMeterRegistry/既有AuthorizationService runtime fixtures/ContextRunner；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 只新增TEST022/023/024的JWT/cache/audit真实分支与固定host adapter、TEST026具名依赖检查；PDP在Step9、UserInfo在Step8、TLS/parity在Step10。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: 真实verifier/cache/producer发出Timer/Counter/Gauge、低基数/unsupported signal、metric抛错结果不变；registry primary/unique选择及缺enabled能力启动失败。
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void host_actual_metric_strategy_and_dependency_wiring() {
    registry=真实SimpleMeterRegistry，adapter=本host固定receiver；通过configured真实manager/SERVICEclient/producer触发分支;
    真实RSA成功/坏签名与受控JWK get HIT/MISS/EXPIRED；Clock推进触发SERVICE临期重获，并发single-flight仍一次;
    assert jwt.verify.duration只包原实际验签（不含Redis/audit delay），audit delivery/receipt age/spool bytes由真实状态产生;
    context runner使用qualified defaultValidator/primary registry且enabled缺producer/registry失败；metric抛错不改typed认证结果;
    // 不在本Step写尚未实现的UserInfo/PDP/TLS测试，后续同文件继续各自RED/GREEN。
}
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,IdpStarterAutoConfigurationTest`；真实verifier/cache/producer发出Timer/Counter/Gauge、低基数/unsupported signal、metric抛错结果不变；registry primary/unique选择及缺enabled能力启动失败。 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 8 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityMetricsComponent.java`

- Purpose: Core port→现有Micrometer，固定SHOUBING角色；本次只完成本Step所属切片。
- Symbols: record；TianquanSecurityMetricsComponent。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，现有Actuator/MeterRegistry/Common Validator/SC port；本host verifier/cache/PDP/producer是现成邻近能力。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 现有Actuator/MeterRegistry/Common Validator/SC port；本host verifier/cache/PDP/producer；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: TianquanSecurityMetricsComponent implements IdpSecurityMetricsRecorder；注入具名Supplier<MeterRegistry>及defaultValidator。
- Input/output and state mapping: host receiver固定SHOUBING；固定不可变method-reference策略表→Timer/Counter/Gauge；只允许有限result/reason category。
- Error and edge behavior: 多registry使用Boot primary/unique，绝不新建第二registry；恶意signal/负duration拒绝schema；运行metric异常只有限安全日志，不吞原验证/PDP异常。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
@Slf4j @RequiredArgsConstructor
class TianquanSecurityMetricsComponent implements IdpSecurityMetricsRecorder {
    @Qualifier("idpMeterRegistrySupplier") final Supplier<MeterRegistry> registry;
    @Qualifier("defaultValidator") final Validator validator;
    record(observation) { new ValidationUtils(validator).validate(observation); 固定Strategy表.get(observation.signal()).accept(observation); }
    // receiver="SHOUBING"，Timer记录Duration，Counter.increment，Gauge读有界spool状态；tags只固定receiver/result类别。
}
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,IdpStarterAutoConfigurationTest`；多registry使用Boot primary/unique，绝不新建第二registry；恶意signal/负duration拒绝schema；运行metric异常只有限安全日志，不吞原验证/PDP异常。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 9 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityMetricsComponent.java`

- Purpose: 同Core port→本host Micrometer，固定JIANSHEN角色；本次只完成本Step所属切片。
- Symbols: record；TianquanSecurityMetricsComponent。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，现有Actuator/MeterRegistry/Common Validator/SC port；本host verifier/cache/PDP/producer是现成邻近能力。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 现有Actuator/MeterRegistry/Common Validator/SC port；本host verifier/cache/PDP/producer；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: TianquanSecurityMetricsComponent implements IdpSecurityMetricsRecorder；注入具名Supplier<MeterRegistry>及defaultValidator。
- Input/output and state mapping: host receiver固定JIANSHEN；固定不可变method-reference策略表→Timer/Counter/Gauge；只允许有限result/reason category。
- Error and edge behavior: 多registry使用Boot primary/unique，绝不新建第二registry；恶意signal/负duration拒绝schema；运行metric异常只有限安全日志，不吞原验证/PDP异常。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
@Slf4j @RequiredArgsConstructor
class TianquanSecurityMetricsComponent implements IdpSecurityMetricsRecorder {
    @Qualifier("idpMeterRegistrySupplier") final Supplier<MeterRegistry> registry;
    @Qualifier("defaultValidator") final Validator validator;
    record(observation) { new ValidationUtils(validator).validate(observation); 固定Strategy表.get(observation.signal()).accept(observation); }
    // receiver="JIANSHEN"，Timer记录Duration，Counter.increment，Gauge读有界spool状态；tags只固定receiver/result类别。
}
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,IdpStarterAutoConfigurationTest`；多registry使用Boot primary/unique，绝不新建第二registry；恶意signal/负duration拒绝schema；运行metric异常只有限安全日志，不吞原验证/PDP异常。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 10 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityObservationConfiguration.java`

- Purpose: 复用Boot primary/unique registry，具名Supplier解决qualified依赖；本次只完成本Step所属切片。
- Symbols: idpMeterRegistrySupplier / idpSecurityMetricsRecorder factory / enabled dependency check；TianquanSecurityObservationConfiguration。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，Step5 Core port、Step6 observer/props、原Actuator/ObjectProvider/defaultValidator；自己的adapter是现成邻近能力。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: Step5 Core port、Step6 observer/props、原Actuator/ObjectProvider/defaultValidator；自己的adapter；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 只接现有Actuator与Core port并检查enabled具名必需能力；endpoint/TLS/profile实际启用在Step10。
- Input/output and state mapping: Spring ObjectProvider<MeterRegistry>解析primary/unique→JDK Supplier闭包；base/local部署属性→trusted HTTPS/TLS/readiness。
- Error and edge behavior: enabled缺registry/producer/key/spool/fallback/Validator启动失败；disabled不要求部署secret；local三重条件之外不接受plaintext；不存在第二MeterRegistry或按exporter名猜注入。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
@Configuration(proxyBeanMethods=false) class TianquanSecurityObservationConfiguration {
    @Bean(name="idpMeterRegistrySupplier") Supplier<MeterRegistry> supplier(ObjectProvider<MeterRegistry> provider) { return () -> provider.getObject(); }
    @Bean(name="idpSecurityMetricsRecorder") qualified supplier/defaultValidator创建Lombok adapter，host receiver固定;
    enabled时要求真实producer/key/spool/fallback/Validator/registry；disabled不增加审计强依赖;
    // 本Step尚不改6profile，不预先执行Step10 HTTPS/local例外/配置parity的新断言。
}
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,IdpStarterAutoConfigurationTest`；enabled缺registry/producer/key/spool/fallback/Validator启动失败；disabled不要求部署secret；local三重条件之外不接受plaintext；不存在第二MeterRegistry或按exporter名猜注入。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 11 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityObservationConfiguration.java`

- Purpose: 同安全schema/依赖标准，绝不创建第二MeterRegistry；本次只完成本Step所属切片。
- Symbols: idpMeterRegistrySupplier / idpSecurityMetricsRecorder factory / enabled dependency check；TianquanSecurityObservationConfiguration。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，Step5 Core port、Step6 observer/props、原Actuator/ObjectProvider/defaultValidator；自己的adapter是现成邻近能力。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: Step5 Core port、Step6 observer/props、原Actuator/ObjectProvider/defaultValidator；自己的adapter；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 只接现有Actuator与Core port并检查enabled具名必需能力；endpoint/TLS/profile实际启用在Step10。
- Input/output and state mapping: Spring ObjectProvider<MeterRegistry>解析primary/unique→JDK Supplier闭包；base/local部署属性→trusted HTTPS/TLS/readiness。
- Error and edge behavior: enabled缺registry/producer/key/spool/fallback/Validator启动失败；disabled不要求部署secret；local三重条件之外不接受plaintext；不存在第二MeterRegistry或按exporter名猜注入。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
@Configuration(proxyBeanMethods=false) class TianquanSecurityObservationConfiguration {
    @Bean(name="idpMeterRegistrySupplier") Supplier<MeterRegistry> supplier(ObjectProvider<MeterRegistry> provider) { return () -> provider.getObject(); }
    @Bean(name="idpSecurityMetricsRecorder") qualified supplier/defaultValidator创建Lombok adapter，host receiver固定;
    enabled时要求真实producer/key/spool/fallback/Validator/registry；disabled不增加审计强依赖;
    // 本Step尚不改6profile，不预先执行Step10 HTTPS/local例外/配置parity的新断言。
}
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,IdpStarterAutoConfigurationTest`；enabled缺registry/producer/key/spool/fallback/Validator启动失败；disabled不要求部署secret；local三重条件之外不接受plaintext；不存在第二MeterRegistry或按exporter名猜注入。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 12 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`

- Purpose: 原spool/Streams增强strict持久/receipt/有界重放，不删远端目录；本次只完成本Step所属切片。
- Symbols: audit.delivery / receipt.age / spool.bytes真实观察overload；TokenUsageAuditComponent。
- Repository evidence: 当前路径存在，已有appendStrict/reference/confirm/replayPending；保留相邻注解/工厂/原异常风格。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 原stage/confirm/replay状态与新Core IdpSecurityMetricsRecorder；具名factory/Lombok兼容参数；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 在已GREEN strict/receipt上只加入Core port真实发出点，旧构造继续noop观察兼容。
- Input/output and state mapping: strict先校验Event→规范内容冲突检查→file force→ATOMIC_MOVE→directory force→XADD确认→SENT marker force；receipt验证后本节点清理。
- Error and edge behavior: strict不允许AtomicMove fallback或stage错误后broker-only返回；XADD/mark失败保留帧并抛原OAUTH_AUDIT_*；STORED/EXPIRED可清理，DEAD隔离计容量；旧sent等时重投同id。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
appendStrict/stage/publish/confirm/replay真实分支→固定oauth.audit.delivery result Counter，保持原异常与先后顺序;
匹配receipt confirmedAt→当前Clock计算非负Duration→oauth.audit.receipt.age；不把超时当确认成功;
原spoolBytes()实际所有evt/sent/dead容量→oauth.audit.spool.bytes gauge snapshot；有界job更新，DEAD不能释放容量;
// 原durability/指纹/MAC/删除守卫不变；recorder异常受限日志，仅观测异常不吞原I/O失败。
// factory具名注入Core port/Clock，旧技术构造走noop，不新增SDK Micrometer或cache/job。
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,IdpStarterAutoConfigurationTest`；strict不允许AtomicMove fallback或stage错误后broker-only返回；XADD/mark失败保留帧并抛原OAUTH_AUDIT_*；STORED/EXPIRED可清理，DEAD隔离计容量；旧sent等时重投同id。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 13 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java`

- Purpose: 显式named/qualified装配，disabled不增加依赖；自身开启缺能力失败；本次只完成本Step所属切片。
- Symbols: Core recorder到既有managed verifier/producer/client的装配；IdpStarterAutoConfiguration。
- Repository evidence: 当前路径存在，已有观察/producer组件Beans；保留相邻注解/工厂/原异常风格。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: Step7 host adapter、已有IdpObservedVerificationComponent/TokenUsageAuditComponent/manager/SERVICEclient；无Micrometer import；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: host真实recorder注入Step5/6观察参与者并将Step7新增audit观察overload接入；旧typed port/disabled模式保留。
- Input/output and state mapping: managed IdpJwtVerifier createConfigured观察组合→旧final USER/SERVICE包装器及HTTP combined→RPC；disabled仍原构造。
- Error and edge behavior: 开启要求真实producer/converter/Validator/host recorder；缺能力失败，不能ConditionalOnBean静默跳过安全能力；避免shared spy造成循环；Servlet filter仅在chain内启用一次。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
enabled时factory注入@Qualifier("idpSecurityMetricsRecorder") Core port，而非MeterRegistry;
configured manager/cache/SERVICE client与managed verifier都复用同一host recorder；tokenUsageAuditComponent加入真实audit状态观察;
disabled时原factory/port与noop观察保留，不对其他host新增strict投递；旧USER/SERVICE final包装器仍只委托IdpJwtVerifier;
验证多个同typeBean具名消歧、Lombok复制Qualifier，缺开启能力不能ConditionalOnBean静默变绿。
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,IdpStarterAutoConfigurationTest`；开启要求真实producer/converter/Validator/host recorder；缺能力失败，不能ConditionalOnBean静默跳过安全能力；避免shared spy造成循环；Servlet filter仅在chain内启用一次。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 14 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpObservedVerificationComponent.java`

- Purpose: 计数owner迁移的必要直接依赖，避免新增producer计数与旧observer计数重复。
- Symbols: `handoff`中的旧oauth.audit.delivery SENT/FAILED发出点。
- Repository evidence: Step6 observer先提供该信号；本Step由原producer在真正stage/mark/confirm发出。
- Dependencies and consumers: 新完成producer Core recorder；旧observe验证结果与strict手交接保持。
- Why now: producer port接通才具有统一owner；最新用户AGENTS必要直接依赖修改授权，PC010记录。
- Contract/signature changes: 无；仅移走同一事实的额外指标，不删oauth.authentication。
- Input/output and state mapping: 原Event handoff与typed outcomes不变，SENT/FAILED只由producer记录。
- Error and edge behavior: producer指标异常仍软失败，审计异常仍Unavailable；不放宽权限。
- Standards impact: MC-SCOPE-001, MC-REUSE-001, MC-PATTERN-001, MC-TEST-001；原业务注解、日志与qualified依赖保持。
- Literal rule enforcement: Rule 4,6,9,11；同一Decorator/State owner，无新框架/层。
- Implementation pseudocode:

```java
handoff(context) {
    validate(context);
    event = generatedConverter.toTarget(context);
    producer.appendStrict(event); // owns STAGED/SENT/FAILED and gauges
    return true;
}
// original failure handling and oauth.authentication emission remain;
// remove only duplicate delivery calls from the observer.
```

- Verification contribution: 真实producer SENT count=1及两host meter tests；Step6回归仍14/0fail。
- After this file: 当前source10路径全部能力完成后再门禁/commit。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`（Maven）；Step11 npm用下列完整 --prefix，无服务进程。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am -Dtest=TianquanSecurityMetricsContractTest,IdpStarterAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test`；该Step `git diff --check` 与精确commit path审查。
- Expected result: RED必须是上述缺失行为断言；GREEN exit0、目标命名tests实际执行且0 failures/errors/非预期skip；无源schema、Bean注入/敏感值或旧机制退化。Step11普通解析不得走临时alias。
- Failure returns to: 本Step对应assert的生产File；schema/DI到对应载体/factory，receipt状态到Step3/4，原合同退化停止当前Step；artifact缺失停止Step11等待可信输入；若需加库/改SQL/外platform或改Spec语义，回用户/Spec。
- Completion criteria: 两host固定receiver的真实registry adapter与具名Supplier可装配；缺能力不noop，SDK无Micrometer新依赖；保存实际RED/GREEN命令、测试report/准确数量、适用MC证据及当前HEAD，没有业务未完成签名/placeholder或外部runtime假成功。
- Rollback: 未部署时只还原本Step相对前置HEAD的精确diff，保留之前Step及未归属改动；已产生审计/spool时禁止删帧/回旧consumer，按§9停止入口并forward-fix。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityMetricsComponent.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityMetricsComponent.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityObservationConfiguration.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityObservationConfiguration.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityMetricsContractTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityMetricsContractTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java`., `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/pom.xml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpObservedVerificationComponent.java`
- Commit: `feat(tianquan): bind security metrics to host registries`；本轮只规划，不执行。



- PC010必要直接依赖修正：新增producer-owned delivery计数后，需移除observer旧SENT/FAILED delivery计数，避免同一handoff重复计数；oauth.authentication仍observer拥有，验证/审计/typed结果不变。该文件已入全局库存，依最新用户必要依赖修改授权补入本Step（10 unique paths）。

### Step 8 — 在原生 UserInfo 返回前完成使用事实投递

- Requirements: REQ-001, REQ-002, REQ-004, REQ-005, REQ-006, REQ-009, REQ-014, REQ-018
- Dependencies: Step 3 已验证并提交, Step 4 已验证并提交, Step 6 已验证并提交, Step 7 已验证并提交
- Baseline state: 前置Step合同已GREEN并提交；本Step缺口：旧真实UserInfo Strategy完成验签/撤销后直接响应，producer调用为零；handoff失败仍返回成功
- Observable outcome: 在原生 UserInfo 返回前完成使用事实投递；以本Step命名真实行为断言证明，错误分支和负作用同时核对。
- End state: SAS UserInfo原验证一次，usage先响应；与token签发/撤销Outbox及原OAuth错误族保持兼容。
- Test-first gate: Required — 旧真实UserInfo Strategy完成验签/撤销后直接响应，producer调用为零；handoff失败仍返回成功；按File顺序签名编译前置后才运行RED，编译/fixture/环境失败不计。
- Manual Checks: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001；按每File Standards impact及本Step验证分别闭合。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityMetricsContractTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: TianquanSecurityMetricsContractTest；shoubing_real_metrics_and_wiring。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，Actuator SimpleMeterRegistry/原native SAS fixtures/真实MapStruct/ContextRunner是现成邻近能力。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: Actuator SimpleMeterRegistry/原native SAS fixtures/真实MapStruct/ContextRunner；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 新增/扩充shoubing_real_metrics_and_wiring方法；TEST-022–026受控行为断言，默认不启PG/Redis/Testcontainers。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: 真实UserInfo success、无AT、坏签名、错issuer/aud、revoked与strict不可用均先审计再原协议响应；只有完整guard success有proof；无第二次decode。
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void native_userinfo_usage_before_response() {
    用原native USER Code S256/nonce fixture、真实UserInfo filter/Strategy与decoder，assert原RSA JWSVerifier.verify恰一次→原guards→appendStrict→响应;
    无AT/坏签名/错issuer/aud/revoked通过原handler得到401/403族，无login redirect；匿名fallback且三refs null；同阶段错误只一Event;
    strict故障期待原OAuth temporarily_unavailable 503与no-store，业务/成功userinfo响应为零；metric exception保持原结果;
    最终crypto Timer仅实际verify；原code/nonce/CSRF/refresh native suites保持，不以mock Strategy整个结果证明;
}
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,OAuthConsentRoundTripTest,OAuthOidcProtocolContractTest,OAuthAuthorizationPersistenceContractTest,OAuthTokenClientCredentialsTest,OAuthProtocolOpenApiContractTest`；真实UserInfo success、无AT、坏签名、错issuer/aud、revoked与strict不可用均先审计再原协议响应；只有完整guard success有proof；无第二次decode。 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/converter/TokenUsageAuditEventConverter.java`

- Purpose: MapStruct+BaseForwardConverter投影到原11字段Event；本次只完成本Step所属切片。
- Symbols: toProof(Jwt verifiedJwt, URI expectedIssuer, contexts) native投影overload；TokenUsageAuditEventConverter。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，Spring已存在Jwt、URI及同一安全references组件；原UserInfo Strategy是现成邻近能力。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: Spring已存在Jwt、URI及同一安全references组件；原UserInfo Strategy；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 同一generated mapper增纯Jwt+expectedIssuer/resource的proof overload；仅原SAS guard后使用，不依赖Admin BO、不新建mapper/类型。
- Input/output and state mapping: ignoreByDefault映射eventId、schemaVersion=1、可信issuer或fallback、resource、refs、result/reasonCode、occurredAt/traceId；proof→Event复用refs。
- Error and edge behavior: tokenRef由producer受控HMAC函数映射issuer/jti；subject/client哈希复用既有策略；capture失败refs全null；禁止setter/JSON往返映射。
- Standards impact: MC-CONVERT-001, MC-BEAN-001, MC-VALID-001, MC-MODEL-001：@Mapper(componentModel="spring")+Common BaseForwardConverter、ignoreByDefault映射；generated实现具名，真实生成代码测试，接口无业务Slf4j/构造注入。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 11：generated mapper及字段明确映射、目标schema手交接验证；不可手工copy/JSON往返替代。
- Implementation pseudocode:

```java
TokenAuditProofBO toProof(Jwt verifiedJwt,@Context URI expectedIssuer,@Context URI resource,@Context TokenUsageAuditComponent references);
// ignoreByDefault：issuer使用server expectedIssuer（原guard已比对iss），resource使用部署URI；subject/client ref从该已可信Jwt的sub/client_id受控hash，tokenRef从expectedIssuer+jti原HMAC函数。
// 普通SDK toProof(VerifiedJwtBO,contexts)/Event11字段映射保持；无Admin反向依赖/重验证/未可信Jwt补证。
// 新overload只为既有SAS不同可信输入表示复用同一projection，Mapper实际生成/JSON/failed匿名由TEST005/008/原UserInfo tests验证。
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,OAuthConsentRoundTripTest,OAuthOidcProtocolContractTest,OAuthAuthorizationPersistenceContractTest,OAuthTokenClientCredentialsTest,OAuthProtocolOpenApiContractTest`；tokenRef由producer受控HMAC函数映射issuer/jti；subject/client哈希复用既有策略；capture失败refs全null；禁止setter/JSON往返映射。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TransactionalOAuthAuthenticationProvider.java`

- Purpose: 原签名/授权/撤销成功后，返回UserInfo之前记录Token使用；不重复JWT验证或改业务结果；本次只完成本Step所属切片。
- Symbols: 原UserInfo Strategy可信事实投影与strict handoff；TransactionalOAuthAuthenticationProvider。
- Repository evidence: 当前路径存在，已有原UserInfo Strategy/Decorator观察点；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 现有UserInfo JwtAuthenticationToken/TrustedRealmBO及原守卫；generated converter 的纯Jwt+URI proof overload；qualified producer/Clock；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 原UserInfo Strategy在guard完成后、响应前同一converter/producer投递；其他grant/provider不混入usage。
- Input/output and state mapping: 原签名→authorization→security/revoke守卫→可信issuer/resource/refs→strict使用事实→UserInfo响应。
- Error and edge behavior: 成功UserInfo前持久handoff；失败使用匿名fallback；不能再调用SDK verify，不能把token签发/撤销DB+Outbox事实替换成usage。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
原UserInfo Strategy完成既有JWT/issuer/authorization/revocation/claim/resource守卫后：
verifiedJwt=原JwtAuthenticationToken中的已验签Jwt；expectedIssuer=原已解析可信realm.issuer，resource=部署receiver URI;
proof=converter.toProof(verifiedJwt,expectedIssuer,resource,既有reference组件)；此overload只投影secret-free值，不重建/重新decode token;
context为合法server eventId/trace/clock与proof，Default schema校验→appendStrict(converter.toTarget(context))→原UserInfo响应;
// 任一守卫失败不生成proof：错误由Step8 userInfoFailureHandler在render前以fallback/null refs审计一次；grant签发/撤销Outbox原样。
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,OAuthConsentRoundTripTest,OAuthOidcProtocolContractTest,OAuthAuthorizationPersistenceContractTest,OAuthTokenClientCredentialsTest,OAuthProtocolOpenApiContractTest`；成功UserInfo前持久handoff；失败使用匿名fallback；不能再调用SDK verify，不能把token签发/撤销DB+Outbox事实替换成usage。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java`

- Purpose: 将四技术preflight纳入同原生链，Cookie CSRF及协议Provider保持；本次只完成本Step所属切片。
- Symbols: protocol matcher/cors object processor；AuthorizationServerConfiguration。
- Repository evidence: 当前路径存在，已有protocol matcher/cors object processor；保留相邻注解/工厂/原异常风格。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 原SAS providers/decoder/JWKSource/Session/CORS；TransactionalOAuthAuthenticationProvider/UserInfo；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 原authorizationServerSecurityFilterChain原protocol matcher OR四原生OPTIONS matcher；CorsFilter processor与UserInfo decoder观察。
- Input/output and state mapping: 原SAS configure保持multipleIssuers/PKCE/CSRF；四OPTIONS无凭据协商；UserInfo真实RSA解码→timer/verified capture同步回调。
- Error and edge behavior: 无Origin OPTIONS也进入限定processor403；实际同源无Origin不拒绝；密码学时间不含DB/Redis/usage；不新增second JwtDecoder。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
tenantAuthorizationServerJwtDecoder原DefaultJWTProcessor保留RS256 JWSVerificationKeySelector、at+jwt type verifier及claims guards;
processor.setJWSVerifierFactory包装原Factory，仅真正JWSVerifier.verify前后记录Duration，其他JCA/header/key/算法能力原样委托;
原NimbusJwtDecoder只创建一次；issuer/authorization/revocation守卫结束后，原UserInfo Strategy在生成响应前appendStrict同源可信事实;
// JWT坏格式/issuer预拒绝没有实际verify调用则不产密码学Timer；使用失败走fallback，无额外SDK decode。
// 先前四OPTIONS matcher、Cookie CSRF/PKCE/nonce/refresh/grant与OAuth错误族均保持。
userInfoFailureHandler(mapper)扩具名producer/converter/配置callback：无Token/坏签名/拒绝/不可用均在原401/403/503 OAuth error body前strict补录，未完整验证只fallback/null refs；已登记同一决策不再另起ID。
原exceptionHandling entrypoint仅GET native UserInfo分支调用该handler而不重定向login；其他authorize/login/consent沿原会话与重定向。

```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,OAuthConsentRoundTripTest,OAuthOidcProtocolContractTest,OAuthAuthorizationPersistenceContractTest,OAuthTokenClientCredentialsTest,OAuthProtocolOpenApiContractTest`；无Origin OPTIONS也进入限定processor403；实际同源无Origin不拒绝；密码学时间不含DB/Redis/usage；不新增second JwtDecoder。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`（Maven）；Step11 npm用下列完整 --prefix，无服务进程。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TianquanSecurityMetricsContractTest,OAuthConsentRoundTripTest,OAuthOidcProtocolContractTest,OAuthAuthorizationPersistenceContractTest,OAuthTokenClientCredentialsTest,OAuthProtocolOpenApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test`；该Step `git diff --check` 与精确commit path审查。
- Expected result: RED必须是上述缺失行为断言；GREEN exit0、目标命名tests实际执行且0 failures/errors/非预期skip；无源schema、Bean注入/敏感值或旧机制退化。Step11普通解析不得走临时alias。
- Failure returns to: 本Step对应assert的生产File；schema/DI到对应载体/factory，receipt状态到Step3/4，原合同退化停止当前Step；artifact缺失停止Step11等待可信输入；若需加库/改SQL/外platform或改Spec语义，回用户/Spec。
- Completion criteria: SAS UserInfo原验证一次，usage先响应；与token签发/撤销Outbox及原OAuth错误族保持兼容；保存实际RED/GREEN命令、测试report/准确数量、适用MC证据及当前HEAD，没有业务未完成签名/placeholder或外部runtime假成功。
- Rollback: 未部署时只还原本Step相对前置HEAD的精确diff，保留之前Step及未归属改动；已产生审计/spool时禁止删帧/回旧consumer，按§9停止入口并forward-fix。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityMetricsContractTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/converter/TokenUsageAuditEventConverter.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TransactionalOAuthAuthenticationProvider.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java`.
- Commit: `fix(shoubing): audit native UserInfo token use`；本轮只规划，不执行。


### Step 9 — 保留 RBAC 可信观察信息并闭合安全异常

- Requirements: REQ-001, REQ-002, REQ-004, REQ-005, REQ-011, REQ-012, REQ-014, REQ-015, REQ-018
- Dependencies: Step 2 已验证并提交, Step 6 已验证并提交, Step 7 已验证并提交
- Baseline state: 前置Step合同已GREEN并提交；本Step缺口：旧RBAC桥不保留proof、PDP无拒绝usage/专用计数，真实JA两chain没有明确错误族；401/403/503/先审计及不同stage ID断言失败
- Observable outcome: 保留 RBAC 可信观察信息并闭合安全异常；以本Step命名真实行为断言证明，错误分支和负作用同时核对。
- End state: 五PDP方法结果/异常原样观察，HTTP/RPC拒绝在返回/渲染前留事实；401/403/503及no-store自有链明确。
- Test-first gate: Required — 旧RBAC桥不保留proof、PDP无拒绝usage/专用计数，真实JA两chain没有明确错误族；401/403/503/先审计及不同stage ID断言失败；按File顺序签名编译前置后才运行RED，编译/fixture/环境失败不计。
- Manual Checks: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001；按每File Standards impact及本Step验证分别闭合。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/Rbac3AdminSecurityChainTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: Rbac3AdminSecurityChainTest；own_admin_security_error_families。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，现有Rbac3AdminSecurityConfigurationTest安全Web slice与真实filter链是现成邻近能力。
- Dependencies and consumers: 现有Rbac3AdminSecurityConfigurationTest安全Web slice与真实filter链；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 新增/扩充own_admin_security_error_families方法；TEST-007受控行为断言，默认不启PG/Redis/Testcontainers。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: 无AT/非法401、权限403、Redis/audit不可用503且业务0；no-store/challenge/无redirect；原Idp→RBAC→tenant finally清理
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void own_admin_security_error_families() {
    arrange 复用现有Rbac3AdminSecurityConfigurationTest安全Web slice与真实filter链，不启动应用或外部基础设施;
    act 调用该Step实际生产入口，不以手工调用record()/mock整段授权替代真实分支;
    assert 无AT/非法401、权限403、Redis/audit不可用503且业务0；no-store/challenge/无redirect；原Idp→RBAC→tenant finally清理;
    verify 所有拒绝/不可用分支businessSpy为零且敏感值不进入event/labels/JSON;
}
```

- Verification contribution: 本Step `Rbac3AdminSecurityChainTest,TianquanSecurityMetricsContractTest,Rbac3AdminSecurityConfigurationTest,Rbac3FieldWriteAuthorizationComponentTest,Rbac3MethodAuthorizationManagerTest`；无AT/非法401、权限403、Redis/audit不可用503且业务0；no-store/challenge/无redirect；原Idp→RBAC→tenant finally清理 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityMetricsContractTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: observed_pdp_preserves_result_and_denied_fact；jianshen_real_metrics_and_wiring。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，Step2真实guard/PDP fixture、Step6 proof/RPC context、Step7 host recorder、Step9 Decorator/renderer是现成邻近能力。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: Step2真实guard/PDP fixture、Step6 proof/RPC context、Step7 host recorder、Step9 Decorator/renderer；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 追加五PDP方法真实delegate、Token proof与独立授权拒绝事件/原异常、有限维度测试；TLS/parity到Step10。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: 五PDP方法结果/异常原样+低基数；Token拒绝不同phase eventId同trace；无Token只metrics；metric故障不改结果；prod TLS/local例外/parity/缺依赖启动失败
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void observed_pdp_preserves_result_and_denied_fact() {
    原真实DefaultAuthorizationService固定runtime snapshot→observed decorator；五原方法分别调用并比较结果/异常identity/reason不变;
    有verified proof时拒绝先strict AUTHORIZATION_DENIED，eventId区别于TOKEN_USE_VERIFIED，同合法trace；无Token本地PDP仅metrics;
    HTTP method抛拒绝→renderer不得再发同一决策Event；RPC在返回/throw前留事实，callback finally恢复;
    registry tags只固定receiver/有限结果类别；metric失败保留原DENY/Unavailable，audit失败禁止业务继续;
}
```

- Verification contribution: 本Step `Rbac3AdminSecurityChainTest,TianquanSecurityMetricsContractTest,Rbac3AdminSecurityConfigurationTest,Rbac3FieldWriteAuthorizationComponentTest,Rbac3MethodAuthorizationManagerTest`；五PDP方法结果/异常原样+低基数；Token拒绝不同phase eventId同trace；无Token只metrics；metric故障不改结果；prod TLS/local例外/parity/缺依赖启动失败 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 3 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/authorization/Rbac3ObservedAuthorizationService.java`

- Purpose: Decorator保留delegate权限/异常，记录实际结果；Token上下文DENY沿既有usage MQ，0新HTTP；本次只完成本Step所属切片。
- Symbols: 原五个AuthorizationService方法；Rbac3ObservedAuthorizationService。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，原AuthorizationService.Default SPI/Idp proof/qualified producer与converter/Clock；method PEP/field guard/HTTP/RPC是现成邻近能力。
- Dependencies and consumers: 原AuthorizationService.Default SPI/Idp proof/qualified producer与converter/Clock；method PEP/field guard/HTTP/RPC；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: implements AuthorizationService，Decorator完整五方法；具名delegate，选择自身enabled观察。
- Input/output and state mapping: 原requirePermission/decideDataScope/decideFields/checkParticipation/verifyFence原结果/异常→固定方法策略观察；有效Token proof下拒绝提交独立授权事实。
- Error and edge behavior: 未Token本地PDP只metrics不伪usage；保持原异常identity/reason；metrics错误不改变结果；audit失败必须Unavailable；不同stage事件不同id同trace。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
@Slf4j @RequiredArgsConstructor class Rbac3ObservedAuthorizationService implements AuthorizationService {
    @Qualifier("rbac3AuthorizationDelegate") final AuthorizationService delegate; @Qualifier("idpSecurityMetricsRecorder") final IdpSecurityMetricsRecorder recorder;
    // 五个方法分别固定method-reference Function/Supplier调用，结果类型的固定Strategy投影；不反射/string dispatch
    decideFields(request) { return 原结果保留的observe(() -> delegate.decideFields(request)); }
    observe(...) { 原调用一次; 记录有限rbac.authorization；只有当前可信proof才strict DENIED Event；throw/return原结果; }
}
```

- Verification contribution: 本Step `Rbac3AdminSecurityChainTest,TianquanSecurityMetricsContractTest,Rbac3AdminSecurityConfigurationTest,Rbac3FieldWriteAuthorizationComponentTest,Rbac3MethodAuthorizationManagerTest`；未Token本地PDP只metrics不伪usage；保持原异常identity/reason；metrics错误不改变结果；audit失败必须Unavailable；不同stage事件不同id同trace。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/security/Rbac3BearerAuthenticationFilter.java`

- Purpose: 在替换Authentication时保留已验证audit proof；RBAC拒绝先记录再写response；本次只完成本Step所属切片。
- Symbols: bridge/拒绝render前callback；Rbac3BearerAuthenticationFilter。
- Repository evidence: 当前路径存在，已有bridge/拒绝render前callback；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: SingleFlightSnapshotLoader/Jackson/Idp proof/审计callback；SA/JA chain；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: Rbac3BearerAuthenticationFilter身份桥保留details/proof；自身enabled拒绝render前调用审计callback。
- Input/output and state mapping: 原Idp auth→Rbac3ContextAuthentication，复制secret-free details；原snapshot/fence/tenant顺序不动。
- Error and edge behavior: 拒绝/不可用先审计再403/503；无proof不伪造issuer；已记录stage标记去重；旧构造factory与disabled路径保持。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
original = SecurityContextHolder.getContext().getAuthentication();
rbacAuth = 原Rbac3ContextAuthentication(身份与snapshot); rbacAuth.setDetails(original.getDetails());
try { 原RBAC链; } catch (原授权异常 failure) { observedCallback记录该阶段; 原renderer(failure); }
finally { 原身份/tenant恢复; }
```

- Verification contribution: 本Step `Rbac3AdminSecurityChainTest,TianquanSecurityMetricsContractTest,Rbac3AdminSecurityConfigurationTest,Rbac3FieldWriteAuthorizationComponentTest,Rbac3MethodAuthorizationManagerTest`；拒绝/不可用先审计再403/503；无proof不伪造issuer；已记录stage标记去重；旧构造factory与disabled路径保持。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/web/Rbac3AuthorizationExceptionHandler.java`

- Purpose: 原两字段异常族，增加正确field guard异常映射/安全no-store；不重包正常响应；本次只完成本Step所属切片。
- Symbols: denied/unavailable；Rbac3AuthorizationExceptionHandler。
- Repository evidence: 当前路径存在，已有denied/unavailable；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 现有授权异常/qualified producer/converter；Rbac3 filter renderer/JA chain/方法异常；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 原Rbac3AuthorizationExceptionHandler code/message403/503族不换wrapper；no-store与观察callback。
- Input/output and state mapping: AuthorizationDeniedException FIELD_ACCESS_DENIED→403原message；RuntimeUnavailableException→503原message；callback在响应前独立stage登记。
- Error and edge behavior: 不泄漏字段名/策略/tenant/private错误；已记录同stage不重复；零Token context不造审计；审计失败走503，保持原HTTP/Controller ResultRecord边界。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
render(AuthorizationDeniedException failure) {
    enabled且当前proof时先recordSecurityDecisionOnce(failure);
    return 原code/message body与HTTP403及Cache-Control:no-store;
}
render(RuntimeUnavailableException failure) { 原HTTP503/code/message/no-store；同样先观察; }
// filter入口异常由Security handler处理，不假设ControllerAdvice能拦截早期filter。
```

- Verification contribution: 本Step `Rbac3AdminSecurityChainTest,TianquanSecurityMetricsContractTest,Rbac3AdminSecurityConfigurationTest,Rbac3FieldWriteAuthorizationComponentTest,Rbac3MethodAuthorizationManagerTest`；不泄漏字段名/策略/tenant/private错误；已记录同stage不重复；零Token context不造审计；审计失败走503，保持原HTTP/Controller ResultRecord边界。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/autoconfigure/Rbac3StarterAutoConfiguration.java`

- Purpose: 原authorizationService Bean名保留，named delegate/observer/field converter；本次只完成本Step所属切片。
- Symbols: authorizationService/guard注册；Rbac3StarterAutoConfiguration。
- Repository evidence: 当前路径存在，已有authorizationService/guard注册；保留相邻注解/工厂/原异常风格。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 旧method interceptor/field module/current user；新guard/observer；Idp auto configure先执行；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 原authorizationService Bean名与所有known callers保持；新增具名field converter/guard与enabled PDP delegate观察。
- Input/output and state mapping: Step2注册guard→原authorizationService；Step9默认实现改具名rbac3AuthorizationDelegate，外露authorizationService按开关wrap；原consumer customBean替代语义保留。
- Error and edge behavior: enabled不允许PDP recorder/producer缺失silent noop；disabled回原路径；避免对delegate自身再次wrap/同type歧义；Lombok Qualified注入通过JS parent config。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
@Bean(name="rbac3FieldWriteAuthorizationComponent") 使用qualified 原authorizationService/currentRbac3User/converter/defaultValidator;
// Step9：原DefaultAuthorizationService以rbac3AuthorizationDelegate供factory内部组合，authorizationService仍唯一对外Bean名
@Bean(name="authorizationService") disabled返回delegate，enabled返回具名依赖完整的Rbac3ObservedAuthorizationService;
// 保留既有ConditionalOnMissingBean用户替换语义；不更改方法PEP/interceptor/field serializer权限策略。
```

- Verification contribution: 本Step `Rbac3AdminSecurityChainTest,TianquanSecurityMetricsContractTest,Rbac3AdminSecurityConfigurationTest,Rbac3FieldWriteAuthorizationComponentTest,Rbac3MethodAuthorizationManagerTest`；enabled不允许PDP recorder/producer缺失silent noop；disabled回原路径；避免对delegate自身再次wrap/同type歧义；Lombok Qualified注入通过JS parent config。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 7 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/Rbac3AdminSecurityConfiguration.java`

- Purpose: 明确entry point/denied handler、自身observer、原Idp→RBAC→tenant顺序；本次只完成本Step所属切片。
- Symbols: 两个原SecurityFilterChain；Rbac3AdminSecurityConfiguration。
- Repository evidence: 当前路径存在，已有两个原SecurityFilterChain；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 原Idp/Rbac3 filters/TenantContextFilter/Jackson；内部SERVICE和Admin HTTP；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 两个SecurityFilterChain配置explicit AuthenticationEntryPoint/AccessDeniedHandler；仅自己启用observation。
- Input/output and state mapping: 无认证401原Idp族；授权403原RBAC族；必需状态/审计503；原internal SERVICE chain与Idp→RBAC→tenant顺序。
- Error and edge behavior: 缺token/非法/故障不能302、成功或堆栈；handler先audit再body，no-store及适当Bearer challenge；内网SERVICE不授予USER字段写。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
rbac3InternalSecurityFilterChain/http chain {
    exceptionHandling明确401 entrypoint与403 denied renderer；503由原typed filter/runtime unavailable保留;
    self enabled接入observation；Idp before Anonymous，RBAC after Idp，tenant after RBAC;
    Bearer csrf disabled/session stateless原样；原Controller ResultRecord成功和业务异常wrapper不改;
}
```

- Verification contribution: 本Step `Rbac3AdminSecurityChainTest,TianquanSecurityMetricsContractTest,Rbac3AdminSecurityConfigurationTest,Rbac3FieldWriteAuthorizationComponentTest,Rbac3MethodAuthorizationManagerTest`；缺token/非法/故障不能302、成功或堆栈；handler先audit再body，no-store及适当Bearer challenge；内网SERVICE不授予USER字段写。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 8 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/IdpSecurityConfig.java`

- Purpose: 保留自身Bearer链，复用错误响应；CORS不开放Admin；本次只完成本Step所属切片。
- Symbols: idpAdminSecurityFilterChain observer与原401/403/503渲染族；IdpSecurityConfig。
- Repository evidence: 当前路径存在，已有CORS/自有观察filter/错误族；保留相邻注解/工厂/原异常风格。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: Step6 IdpSecurityObservationFilter/Idp proof；Step9 RBAC decorator/renderer；原Jackson与endpointPolicy；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 仅本Admin安全链启用观察filter和pre-render拒绝审计，原Step1 CORS source不再改。
- Input/output and state mapping: 原allowedOrigins→四route POST或GET、credentials=false；原Idp→RBAC链保持，新增observer仅自身。
- Error and edge behavior: health/preflight不产生usage；Admin同源不开放CORS；401/403/503必须filter层处理，OAuth原错误族不换wrapper。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
idpAdminSecurityFilterChain(http,具名原filters,具名observer,qualified Jackson) {
    enabled在Idp前加入request observation容器，原Idp→RBAC顺序不变，禁Servlet重复注册;
    明确401 entrypoint/403 denied/原Unavailable503，先调用尚未记录决策的audit callback再no-store/安全body;
    已PDP记录的DENIED render不再另起Event；失败补录只用server fallback或完整Valid proof;
    // 原同源Admin及Step1四资源CORS source保持；Controller成功/ResultRecord不改。
}
```

- Verification contribution: 本Step `Rbac3AdminSecurityChainTest,TianquanSecurityMetricsContractTest,Rbac3AdminSecurityConfigurationTest,Rbac3FieldWriteAuthorizationComponentTest,Rbac3MethodAuthorizationManagerTest`；health/preflight不产生usage；Admin同源不开放CORS；401/403/503必须filter层处理，OAuth原错误族不换wrapper。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`（Maven）；Step11 npm用下列完整 --prefix，无服务进程。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am -Dtest=Rbac3AdminSecurityChainTest,TianquanSecurityMetricsContractTest,Rbac3AdminSecurityConfigurationTest,Rbac3FieldWriteAuthorizationComponentTest,Rbac3MethodAuthorizationManagerTest -Dsurefire.failIfNoSpecifiedTests=false test`；该Step `git diff --check` 与精确commit path审查。
- Expected result: RED必须是上述缺失行为断言；GREEN exit0、目标命名tests实际执行且0 failures/errors/非预期skip；无源schema、Bean注入/敏感值或旧机制退化。Step11普通解析不得走临时alias。
- Failure returns to: 本Step对应assert的生产File；schema/DI到对应载体/factory，receipt状态到Step3/4，原合同退化停止当前Step；artifact缺失停止Step11等待可信输入；若需加库/改SQL/外platform或改Spec语义，回用户/Spec。
- Completion criteria: 五PDP方法结果/异常原样观察，HTTP/RPC拒绝在返回/渲染前留事实；401/403/503及no-store自有链明确；保存实际RED/GREEN命令、测试report/准确数量、适用MC证据及当前HEAD，没有业务未完成签名/placeholder或外部runtime假成功。
- Rollback: 未部署时只还原本Step相对前置HEAD的精确diff，保留之前Step及未归属改动；已产生审计/spool时禁止删帧/回旧consumer，按§9停止入口并forward-fix。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/Rbac3AdminSecurityChainTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityMetricsContractTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/authorization/Rbac3ObservedAuthorizationService.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/security/Rbac3BearerAuthenticationFilter.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/web/Rbac3AuthorizationExceptionHandler.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/autoconfigure/Rbac3StarterAutoConfiguration.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/Rbac3AdminSecurityConfiguration.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/IdpSecurityConfig.java`.
- Commit: `fix(jianshen): preserve audit proof and security errors`；本轮只规划，不执行。


### Step 10 — 只在两个自身 Admin 启用并验证配置与 TLS 门禁

- Requirements: REQ-001, REQ-002, REQ-006, REQ-014, REQ-015, REQ-016, REQ-018, REQ-019
- Dependencies: Step 1 已验证并提交, Step 2 已验证并提交, Step 4 已验证并提交, Step 6 已验证并提交, Step 7 已验证并提交, Step 8 已验证并提交, Step 9 已验证并提交
- Baseline state: 前置Step合同已GREEN并提交；本Step缺口：现有profile无usage/delivery一致键且RPC/Tianshu生产TLS默认false；受控binder/parity/TLS拒绝与缺依赖启动失败断言失败
- Observable outcome: 只在两个自身 Admin 启用并验证配置与 TLS 门禁；以本Step命名真实行为断言证明，错误分支和负作用同时核对。
- End state: self base/local开启、offline/test显式关闭或override；四种部署/例外条件可验证，其他host默认关闭；runtime仍未验收。
- Test-first gate: Required — 现有profile无usage/delivery一致键且RPC/Tianshu生产TLS默认false；受控binder/parity/TLS拒绝与缺依赖启动失败断言失败；按File顺序签名编译前置后才运行RED，编译/fixture/环境失败不计。
- Manual Checks: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001；按每File Standards impact及本Step验证分别闭合。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。
- Ordered files:

#### File 1 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityMetricsContractTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: own_profile_parity_and_tls_readiness；shoubing_real_metrics_and_wiring。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，本host原6profile YAML/Core Props/原Environment/RPC/Tianshu TLS配置及Step10 ObservationConfiguration是现成邻近能力。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 本host原6profile YAML/Core Props/原Environment/RPC/Tianshu TLS配置及Step10 ObservationConfiguration；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 仅新增TEST026各profile键同构、HTTPS/TLS/local例外与缺开启依赖的ContextRunner负例；保留前置GREEN测试。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: JWT成功失败Timer excludes Redis/audit；真实cache counter与SERVICE acquire；UserInfo一次验签先handoff；低基数与metric抛错不改结果；base/local/offline配置与qualified注入
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void own_profile_parity_and_tls_readiness() {
    用现有YamlPropertySourceLoader/Boot Binder加载本host base/local/offline或local-it，比较Spec §15核心叶子，值可不同但keys一致;
    context runner只提供具名test doubles/真实Validator/registry，验证base生产HTTPS/rediss/RPC+Tianshu TLS，否则enabled启动失败;
    local plaintext仅profile local且DEPLOYMENT_ENV local/dev且显式allow条件；prod/profile外/任意hostname仍拒绝，issuer始终HTTPS;
    offline/test明确disabled时不强依赖producer/key/registry、不启动任何真实infra；反向enabled缺能力必须拒绝;
}
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,Rbac3AdminSecurityChainTest,IdpStarterAutoConfigurationTest,SpringConstructorResolutionTest`；JWT成功失败Timer excludes Redis/audit；真实cache counter与SERVICE acquire；UserInfo一次验签先handoff；低基数与metric抛错不改结果；base/local/offline配置与qualified注入 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 2 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityMetricsContractTest.java`

- Purpose: §14对应真实行为/范围回归，默认不启动外部设施；本次只完成本Step所属切片。
- Symbols: own_profile_parity_and_tls_readiness；jianshen_real_metrics_and_wiring。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，本host原6profile YAML/Core Props/原Environment/RPC/Tianshu TLS配置及Step10 ObservationConfiguration是现成邻近能力。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 本host原6profile YAML/Core Props/原Environment/RPC/Tianshu TLS配置及Step10 ObservationConfiguration；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 仅新增TEST026各profile键同构、HTTPS/TLS/local例外与缺开启依赖的ContextRunner负例；保留前置GREEN测试。
- Input/output and state mapping: 固定可信issuer/RSA/Clock、实际目标组件→结果、调用顺序、event/frame/receipt、registry；库状态仅test double，非部署证据。
- Error and edge behavior: 五PDP方法结果/异常原样+低基数；Token拒绝不同phase eventId同trace；无Token只metrics；metric故障不改结果；prod TLS/local例外/parity/缺依赖启动失败
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```java
@Test void own_profile_parity_and_tls_readiness() {
    用现有YamlPropertySourceLoader/Boot Binder加载本host base/local/offline或local-it，比较Spec §15核心叶子，值可不同但keys一致;
    context runner只提供具名test doubles/真实Validator/registry，验证base生产HTTPS/rediss/RPC+Tianshu TLS，否则enabled启动失败;
    local plaintext仅profile local且DEPLOYMENT_ENV local/dev且显式allow条件；prod/profile外/任意hostname仍拒绝，issuer始终HTTPS;
    offline/test明确disabled时不强依赖producer/key/registry、不启动任何真实infra；反向enabled缺能力必须拒绝;
}
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,Rbac3AdminSecurityChainTest,IdpStarterAutoConfigurationTest,SpringConstructorResolutionTest`；五PDP方法结果/异常原样+低基数；Token拒绝不同phase eventId同trace；无Token只metrics；metric故障不改结果；prod TLS/local例外/parity/缺依赖启动失败 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityObservationConfiguration.java`

- Purpose: 复用Boot primary/unique registry，具名Supplier解决qualified依赖；本次只完成本Step所属切片。
- Symbols: metrics/observer/readiness Beans；TianquanSecurityObservationConfiguration。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，本host现有Actuator/Environment/IdpStarterProperties/Rbac3StarterProperties/原RPC与Tianshu props；两SDK auto装配是现成邻近能力。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 本host现有Actuator/Environment/IdpStarterProperties/Rbac3StarterProperties/原RPC与Tianshu props；两SDK auto装配；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 具名idpMeterRegistrySupplier/idpSecurityMetricsRecorder与enabled readiness验证；技术@Configuration factory有qualified参数。
- Input/output and state mapping: Spring ObjectProvider<MeterRegistry>解析primary/unique→JDK Supplier闭包；base/local部署属性→trusted HTTPS/TLS/readiness。
- Error and edge behavior: enabled缺registry/producer/key/spool/fallback/Validator启动失败；disabled不要求部署secret；local三重条件之外不接受plaintext；不存在第二MeterRegistry或按exporter名猜注入。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
@Configuration(proxyBeanMethods=false) class TianquanSecurityObservationConfiguration {
    @Bean(name="idpMeterRegistrySupplier") Supplier<MeterRegistry> supplier(ObjectProvider<MeterRegistry> provider) { return () -> provider.getObject(); }
    @Bean(name="idpSecurityMetricsRecorder") 返回Lombok构造的adapter，qualified Supplier/defaultValidator;
    enabled检查：可信fallback issuer+public/resource/provider/Jianshen endpoint HTTPS，安全PRIMARY rediss，自有启用RPC/Tianshu TLS flags;
    local例外仅active local AND DEPLOYMENT_ENV=local/dev AND allowLocalPlaintext，issuer仍HTTPS;
    // 原readiness聚合卷/key/DEAD/满容量故障，不新建HTTP探针或health服务。
}
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,Rbac3AdminSecurityChainTest,IdpStarterAutoConfigurationTest,SpringConstructorResolutionTest`；enabled缺registry/producer/key/spool/fallback/Validator启动失败；disabled不要求部署secret；local三重条件之外不接受plaintext；不存在第二MeterRegistry或按exporter名猜注入。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityObservationConfiguration.java`

- Purpose: 同安全schema/依赖标准，绝不创建第二MeterRegistry；本次只完成本Step所属切片。
- Symbols: metrics/observer/readiness Beans；TianquanSecurityObservationConfiguration。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，本host现有Actuator/Environment/IdpStarterProperties/Rbac3StarterProperties/原RPC与Tianshu props；两SDK auto装配是现成邻近能力。 此路径已由前置File/Step创建或修改，本次仅扩表列符号，不重置先前GREEN。
- Dependencies and consumers: 本host现有Actuator/Environment/IdpStarterProperties/Rbac3StarterProperties/原RPC与Tianshu props；两SDK auto装配；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 具名idpMeterRegistrySupplier/idpSecurityMetricsRecorder与enabled readiness验证；技术@Configuration factory有qualified参数。
- Input/output and state mapping: Spring ObjectProvider<MeterRegistry>解析primary/unique→JDK Supplier闭包；base/local部署属性→trusted HTTPS/TLS/readiness。
- Error and edge behavior: enabled缺registry/producer/key/spool/fallback/Validator启动失败；disabled不要求部署secret；local三重条件之外不接受plaintext；不存在第二MeterRegistry或按exporter名猜注入。
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001：具体业务类@Slf4j、@RequiredArgsConstructor final qualified dependencies及显式Bean名；技术factory所有依赖参数@Qualifier；原compat API构造仅适配，注入走Lombok；JDK/Jackson/java.time与上述选定pattern、schema边界。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：本File contract与pseudocode落实具名qualified组合、状态/结果/异常、无secret日志与UTC/Duration；technical factory/Context不伪装业务数据class，无新Utils/依赖。
- Implementation pseudocode:

```java
@Configuration(proxyBeanMethods=false) class TianquanSecurityObservationConfiguration {
    @Bean(name="idpMeterRegistrySupplier") Supplier<MeterRegistry> supplier(ObjectProvider<MeterRegistry> provider) { return () -> provider.getObject(); }
    @Bean(name="idpSecurityMetricsRecorder") 返回Lombok构造的adapter，qualified Supplier/defaultValidator;
    enabled检查：可信fallback issuer+public/resource/provider/Jianshen endpoint HTTPS，安全PRIMARY rediss，自有启用RPC/Tianshu TLS flags;
    local例外仅active local AND DEPLOYMENT_ENV=local/dev AND allowLocalPlaintext，issuer仍HTTPS;
    // 原readiness聚合卷/key/DEAD/满容量故障，不新建HTTP探针或health服务。
}
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,Rbac3AdminSecurityChainTest,IdpStarterAutoConfigurationTest,SpringConstructorResolutionTest`；enabled缺registry/producer/key/spool/fallback/Validator启动失败；disabled不要求部署secret；local三重条件之外不接受plaintext；不存在第二MeterRegistry或按exporter名猜注入。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml`

- Purpose: §15同构；自身web启用，offline关闭；本次只完成本Step所属切片。
- Symbols: 配置核心叶子键；application。
- Repository evidence: 当前路径存在，已有配置核心叶子键；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 两个Properties/host readiness；现有RPC/Tianshu props，不修改这些组件或其他平台；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: SHOUBING application.yml核心键同构；新增usage-observation及audit.delivery，既有RPC/Tianshu TLS默认修正。
- Input/output and state mapping: enabled=true、fallback trusted receiver URI、allow-local-plaintext=false；receipt-ttl PT1H、timeout PT30S、batch100；其他值继续既有env secret引用。
- Error and edge behavior: migration/local-it关闭web observation不隐启infra；base rpc.tls.enabled / tianshu.rpc.tls.enabled=true与development-plaintext=false；local按明确受限例外override；SA allowed-origins精确，JA不加SA协议key。
- Standards impact: MC-CONFIG-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001：配置/lock是数据文件，Java POJO/日志注解不适用；保持同构/真实制品校验和当前owner，无secret值。
- Literal rule enforcement: Rule 7, Rule 11：同核心key或既有artifact声明不变，范围仅本平台；Rule 6：保留JSON/YAML既有字段和无敏感制品输出。
- Implementation pseudocode:

```yaml
# application.yml，以下叶子在两个host所有列出profile均表示；值可以不同
usage-observation: {enabled: true, fallback-issuer: 既有可信issuer部署引用, allow-local-plaintext: false}
audit.delivery: {receipt-ttl: PT1H, confirmation-timeout: PT30S, batch-size: 100}
# 保持实际全prefix egon.cola.platform.tianquan.shoubing；未开启不要求secrets；本平台已有egon.cola.component.rpc.tls及tianshu.rpc.tls生产默认true。
# 不新增server/证书实现、不改变DDL或tenant ignored-table、保留原CSRF与所有原部署引用。
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,Rbac3AdminSecurityChainTest,IdpStarterAutoConfigurationTest,SpringConstructorResolutionTest`；migration/local-it关闭web observation不隐启infra；base rpc.tls.enabled / tianshu.rpc.tls.enabled=true与development-plaintext=false；local按明确受限例外override；SA allowed-origins精确，JA不加SA协议key。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml`

- Purpose: §15同构；自身web启用，offline关闭；本次只完成本Step所属切片。
- Symbols: 配置核心叶子键；application-local。
- Repository evidence: 当前路径存在，已有配置核心叶子键；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 两个Properties/host readiness；现有RPC/Tianshu props，不修改这些组件或其他平台；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: SHOUBING application-local.yml核心键同构；新增usage-observation及audit.delivery，既有RPC/Tianshu TLS默认修正。
- Input/output and state mapping: enabled=true、fallback trusted receiver URI、allow-local-plaintext=true（仅local+部署env=local/dev）；receipt-ttl PT1H、timeout PT30S、batch100；其他值继续既有env secret引用。
- Error and edge behavior: migration/local-it关闭web observation不隐启infra；base rpc.tls.enabled / tianshu.rpc.tls.enabled=true与development-plaintext=false；local按明确受限例外override；SA allowed-origins精确，JA不加SA协议key。
- Standards impact: MC-CONFIG-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001：配置/lock是数据文件，Java POJO/日志注解不适用；保持同构/真实制品校验和当前owner，无secret值。
- Literal rule enforcement: Rule 7, Rule 11：同核心key或既有artifact声明不变，范围仅本平台；Rule 6：保留JSON/YAML既有字段和无敏感制品输出。
- Implementation pseudocode:

```yaml
# application-local.yml，以下叶子在两个host所有列出profile均表示；值可以不同
usage-observation: {enabled: true, fallback-issuer: 既有可信issuer部署引用, allow-local-plaintext: true（仅local+部署env=local/dev）}
audit.delivery: {receipt-ttl: PT1H, confirmation-timeout: PT30S, batch-size: 100}
# 保持实际全prefix egon.cola.platform.tianquan.shoubing；未开启不要求secrets；本平台已有egon.cola.component.rpc.tls及tianshu.rpc.tls生产默认true。
# 不新增server/证书实现、不改变DDL或tenant ignored-table、保留原CSRF与所有原部署引用。
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,Rbac3AdminSecurityChainTest,IdpStarterAutoConfigurationTest,SpringConstructorResolutionTest`；migration/local-it关闭web observation不隐启infra；base rpc.tls.enabled / tianshu.rpc.tls.enabled=true与development-plaintext=false；local按明确受限例外override；SA allowed-origins精确，JA不加SA协议key。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 7 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-oauth-migration.yml`

- Purpose: §15同构；自身web启用，offline关闭；本次只完成本Step所属切片。
- Symbols: 配置核心叶子键；application-oauth-migration。
- Repository evidence: 当前路径存在，已有配置核心叶子键；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 两个Properties/host readiness；现有RPC/Tianshu props，不修改这些组件或其他平台；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: SHOUBING application-oauth-migration.yml核心键同构；新增usage-observation及audit.delivery，既有RPC/Tianshu TLS默认修正。
- Input/output and state mapping: enabled=false、fallback trusted receiver URI、allow-local-plaintext=false；receipt-ttl PT1H、timeout PT30S、batch100；其他值继续既有env secret引用。
- Error and edge behavior: migration/local-it关闭web observation不隐启infra；base rpc.tls.enabled / tianshu.rpc.tls.enabled=true与development-plaintext=false；local按明确受限例外override；SA allowed-origins精确，JA不加SA协议key。
- Standards impact: MC-CONFIG-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001：配置/lock是数据文件，Java POJO/日志注解不适用；保持同构/真实制品校验和当前owner，无secret值。
- Literal rule enforcement: Rule 7, Rule 11：同核心key或既有artifact声明不变，范围仅本平台；Rule 6：保留JSON/YAML既有字段和无敏感制品输出。
- Implementation pseudocode:

```yaml
# application-oauth-migration.yml，以下叶子在两个host所有列出profile均表示；值可以不同
usage-observation: {enabled: false, fallback-issuer: 既有可信issuer部署引用, allow-local-plaintext: false}
audit.delivery: {receipt-ttl: PT1H, confirmation-timeout: PT30S, batch-size: 100}
# 保持实际全prefix egon.cola.platform.tianquan.shoubing；未开启不要求secrets；本平台已有egon.cola.component.rpc.tls及tianshu.rpc.tls生产默认true。
# 不新增server/证书实现、不改变DDL或tenant ignored-table、保留原CSRF与所有原部署引用。
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,Rbac3AdminSecurityChainTest,IdpStarterAutoConfigurationTest,SpringConstructorResolutionTest`；migration/local-it关闭web observation不隐启infra；base rpc.tls.enabled / tianshu.rpc.tls.enabled=true与development-plaintext=false；local按明确受限例外override；SA allowed-origins精确，JA不加SA协议key。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 8 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/application.yml`

- Purpose: §15同构；自身web启用，offline关闭；本次只完成本Step所属切片。
- Symbols: 配置核心叶子键；application。
- Repository evidence: 当前路径存在，已有配置核心叶子键；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 两个Properties/host readiness；现有RPC/Tianshu props，不修改这些组件或其他平台；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: JIANSHEN application.yml核心键同构；新增usage-observation及audit.delivery，既有RPC/Tianshu TLS默认修正。
- Input/output and state mapping: enabled=true、fallback trusted receiver URI、allow-local-plaintext=false；receipt-ttl PT1H、timeout PT30S、batch100；其他值继续既有env secret引用。
- Error and edge behavior: migration/local-it关闭web observation不隐启infra；base rpc.tls.enabled / tianshu.rpc.tls.enabled=true与development-plaintext=false；local按明确受限例外override；SA allowed-origins精确，JA不加SA协议key。
- Standards impact: MC-CONFIG-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001：配置/lock是数据文件，Java POJO/日志注解不适用；保持同构/真实制品校验和当前owner，无secret值。
- Literal rule enforcement: Rule 7, Rule 11：同核心key或既有artifact声明不变，范围仅本平台；Rule 6：保留JSON/YAML既有字段和无敏感制品输出。
- Implementation pseudocode:

```yaml
# application.yml，以下叶子在两个host所有列出profile均表示；值可以不同
usage-observation: {enabled: true, fallback-issuer: 既有可信issuer部署引用, allow-local-plaintext: false}
audit.delivery: {receipt-ttl: PT1H, confirmation-timeout: PT30S, batch-size: 100}
# 保持实际全prefix egon.cola.platform.tianquan.shoubing；未开启不要求secrets；本平台已有egon.cola.component.rpc.tls及tianshu.rpc.tls生产默认true。
# 不新增server/证书实现、不改变DDL或tenant ignored-table、保留原CSRF与所有原部署引用。
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,Rbac3AdminSecurityChainTest,IdpStarterAutoConfigurationTest,SpringConstructorResolutionTest`；migration/local-it关闭web observation不隐启infra；base rpc.tls.enabled / tianshu.rpc.tls.enabled=true与development-plaintext=false；local按明确受限例外override；SA allowed-origins精确，JA不加SA协议key。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 9 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/application-local.yml`

- Purpose: §15同构；自身web启用，offline关闭；本次只完成本Step所属切片。
- Symbols: 配置核心叶子键；application-local。
- Repository evidence: 当前路径存在，已有配置核心叶子键；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 两个Properties/host readiness；现有RPC/Tianshu props，不修改这些组件或其他平台；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: JIANSHEN application-local.yml核心键同构；新增usage-observation及audit.delivery，既有RPC/Tianshu TLS默认修正。
- Input/output and state mapping: enabled=true、fallback trusted receiver URI、allow-local-plaintext=true（仅local+部署env=local/dev）；receipt-ttl PT1H、timeout PT30S、batch100；其他值继续既有env secret引用。
- Error and edge behavior: migration/local-it关闭web observation不隐启infra；base rpc.tls.enabled / tianshu.rpc.tls.enabled=true与development-plaintext=false；local按明确受限例外override；SA allowed-origins精确，JA不加SA协议key。
- Standards impact: MC-CONFIG-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001：配置/lock是数据文件，Java POJO/日志注解不适用；保持同构/真实制品校验和当前owner，无secret值。
- Literal rule enforcement: Rule 7, Rule 11：同核心key或既有artifact声明不变，范围仅本平台；Rule 6：保留JSON/YAML既有字段和无敏感制品输出。
- Implementation pseudocode:

```yaml
# application-local.yml，以下叶子在两个host所有列出profile均表示；值可以不同
usage-observation: {enabled: true, fallback-issuer: 既有可信issuer部署引用, allow-local-plaintext: true（仅local+部署env=local/dev）}
audit.delivery: {receipt-ttl: PT1H, confirmation-timeout: PT30S, batch-size: 100}
# 保持实际全prefix egon.cola.platform.tianquan.shoubing；未开启不要求secrets；本平台已有egon.cola.component.rpc.tls及tianshu.rpc.tls生产默认true。
# 不新增server/证书实现、不改变DDL或tenant ignored-table、保留原CSRF与所有原部署引用。
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,Rbac3AdminSecurityChainTest,IdpStarterAutoConfigurationTest,SpringConstructorResolutionTest`；migration/local-it关闭web observation不隐启infra；base rpc.tls.enabled / tianshu.rpc.tls.enabled=true与development-plaintext=false；local按明确受限例外override；SA allowed-origins精确，JA不加SA协议key。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

#### File 10 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/resources/application-local-it.yml`

- Purpose: 继承/显式override parity，不隐启服务；本次只完成本Step所属切片。
- Symbols: 测试profile核心键；application-local-it。
- Repository evidence: 当前路径存在，已有测试profile核心键；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: 两个Properties/host readiness；现有RPC/Tianshu props，不修改这些组件或其他平台；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: JIANSHEN application-local-it.yml核心键同构；新增usage-observation及audit.delivery，既有RPC/Tianshu TLS默认修正。
- Input/output and state mapping: enabled=false、fallback trusted receiver URI、allow-local-plaintext=false；receipt-ttl PT1H、timeout PT30S、batch100；其他值继续既有env secret引用。
- Error and edge behavior: migration/local-it关闭web observation不隐启infra；base rpc.tls.enabled / tianshu.rpc.tls.enabled=true与development-plaintext=false；local按明确受限例外override；SA allowed-origins精确，JA不加SA协议key。
- Standards impact: MC-CONFIG-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001：配置/lock是数据文件，Java POJO/日志注解不适用；保持同构/真实制品校验和当前owner，无secret值。
- Literal rule enforcement: Rule 7, Rule 11：同核心key或既有artifact声明不变，范围仅本平台；Rule 6：保留JSON/YAML既有字段和无敏感制品输出。
- Implementation pseudocode:

```yaml
# application-local-it.yml，以下叶子在两个host所有列出profile均表示；值可以不同
usage-observation: {enabled: false, fallback-issuer: 既有可信issuer部署引用, allow-local-plaintext: false}
audit.delivery: {receipt-ttl: PT1H, confirmation-timeout: PT30S, batch-size: 100}
# 保持实际全prefix egon.cola.platform.tianquan.shoubing；未开启不要求secrets；本平台已有egon.cola.component.rpc.tls及tianshu.rpc.tls生产默认true。
# 不新增server/证书实现、不改变DDL或tenant ignored-table、保留原CSRF与所有原部署引用。
```

- Verification contribution: 本Step `TianquanSecurityMetricsContractTest,Rbac3AdminSecurityChainTest,IdpStarterAutoConfigurationTest,SpringConstructorResolutionTest`；migration/local-it关闭web observation不隐启infra；base rpc.tls.enabled / tianshu.rpc.tls.enabled=true与development-plaintext=false；local按明确受限例外override；SA allowed-origins精确，JA不加SA协议key。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`（Maven）；Step11 npm用下列完整 --prefix，无服务进程。
- Verification command: `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am -Dtest=TianquanSecurityMetricsContractTest,Rbac3AdminSecurityChainTest,IdpStarterAutoConfigurationTest,SpringConstructorResolutionTest -Dsurefire.failIfNoSpecifiedTests=false test`；该Step `git diff --check` 与精确commit path审查。
- Expected result: RED必须是上述缺失行为断言；GREEN exit0、目标命名tests实际执行且0 failures/errors/非预期skip；无源schema、Bean注入/敏感值或旧机制退化。Step11普通解析不得走临时alias。
- Failure returns to: 本Step对应assert的生产File；schema/DI到对应载体/factory，receipt状态到Step3/4，原合同退化停止当前Step；artifact缺失停止Step11等待可信输入；若需加库/改SQL/外platform或改Spec语义，回用户/Spec。
#### File 11 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/IdpPlatformConfiguration.java`

- Purpose: PC-011 必要直接依赖；将本Step audit guard组合入原自身发布就绪门禁。
- Symbols: idpRuntimeReadiness；原 Bean factory，不新增公共 SPI。
- Repository evidence: 当前文件/具名 readiness factory 存在；原 guard 未消费 audit 状态。
- Dependencies and consumers: 本host `tianquanSecurityAuditReady` BooleanSupplier；原 DDC/密钥/数据库/撤销 guard；现有调用者仅 Spring factory。
- Why now: 观察已由前置Step提交；本Step启用必须同步避免未就绪仍发布。
- Contract/signature changes: factory追加 qualified optional ObjectProvider<BooleanSupplier>；原返回协议不变。
- Input/output and state mapping: enabled supplier当前事实→额外AND检查；disabled provider absent→true，原检查照常决定。
- Error and edge behavior: audit false不能覆盖原失败，原失败不能被audit true解除；不新增HTTP或数据库访问。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-BEAN-001, MC-CONFIG-001, MC-SCOPE-001；原配置组合边界，新增factory参数明确Qualifier。
- Literal rule enforcement: Rule 4 qualified技术factory；Rule 7 profiles；Rule 11 原结构/无SQL。
- Implementation pseudocode:

```java
@Qualifier("tianquanSecurityAuditReady") ObjectProvider<BooleanSupplier> auditReady;
return () -> {
    原DDC/密钥/数据库/撤销检查按原方法执行并保留 Context finally;
    boolean originalReady = 原检查输出;
    BooleanSupplier guard = auditReady.getIfAvailable(() -> () -> true);
    boolean auditStoreReady = guard.getAsBoolean();
    return 原协议(originalReady && auditStoreReady, 其余原字段);
};
```

- Verification contribution: Step10 exact受控命令编译两factory；真实 lease/DEAD/full/pending负例证明supplier，原fixture保持；真实发布仍Runtime unverified。
- After this file: 追加guard已消费，完整Step gate通过后提交。

#### File 12 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/runtime/Rbac3PlatformIntegrationConfiguration.java`

- Purpose: PC-011 必要直接依赖；将本Step audit guard组合入原自身发布就绪门禁。
- Symbols: rbac3ReadinessIndicator；原 Bean factory，不新增公共 SPI。
- Repository evidence: 当前文件/具名 readiness factory 存在；原 guard 未消费 audit 状态。
- Dependencies and consumers: 本host `tianquanSecurityAuditReady` BooleanSupplier；原 DDC/密钥/数据库/撤销 guard；现有调用者仅 Spring factory。
- Why now: 观察已由前置Step提交；本Step启用必须同步避免未就绪仍发布。
- Contract/signature changes: factory追加 qualified optional ObjectProvider<BooleanSupplier>；原返回协议不变。
- Input/output and state mapping: enabled supplier当前事实→额外AND检查；disabled provider absent→true，原检查照常决定。
- Error and edge behavior: audit false不能覆盖原失败，原失败不能被audit true解除；不新增HTTP或数据库访问。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-BEAN-001, MC-CONFIG-001, MC-SCOPE-001；原配置组合边界，新增factory参数明确Qualifier。
- Literal rule enforcement: Rule 4 qualified技术factory；Rule 7 profiles；Rule 11 原结构/无SQL。
- Implementation pseudocode:

```java
@Qualifier("tianquanSecurityAuditReady") ObjectProvider<BooleanSupplier> auditReady;
return () -> {
    原DDC/密钥/数据库/撤销检查按原方法执行并保留 Context finally;
    boolean originalReady = 原检查输出;
    BooleanSupplier guard = auditReady.getIfAvailable(() -> () -> true);
    boolean auditStoreReady = guard.getAsBoolean();
    return 原协议(originalReady && auditStoreReady, 其余原字段);
};
```

- Verification contribution: Step10 exact受控命令编译两factory；真实 lease/DEAD/full/pending负例证明supplier，原fixture保持；真实发布仍Runtime unverified。
- After this file: 追加guard已消费，完整Step gate通过后提交。

- Completion criteria: self base/local开启、offline/test显式关闭或override；四种部署/例外条件可验证，其他host默认关闭；runtime仍未验收；保存实际RED/GREEN命令、测试report/准确数量、适用MC证据及当前HEAD，没有业务未完成签名/placeholder或外部runtime假成功。
- Rollback: 未部署时只还原本Step相对前置HEAD的精确diff，保留之前Step及未归属改动；已产生审计/spool时禁止删帧/回旧consumer，按§9停止入口并forward-fix。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityMetricsContractTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityMetricsContractTest.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityObservationConfiguration.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityObservationConfiguration.java`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-oauth-migration.yml`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/application.yml`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/application-local.yml`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/resources/application-local-it.yml`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/IdpPlatformConfiguration.java`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/runtime/Rbac3PlatformIntegrationConfiguration.java`.
- Commit: `fix(tianquan): enable own security readiness gates`；本轮只规划，不执行。


### Step 11 — 校验自身前端制品并完成范围与回归交付门禁

- Requirements: REQ-001, REQ-002, REQ-014, REQ-017, REQ-018, REQ-019；保持前置ALIGN约束（只边界检查、不扩scope）：REQ-020, REQ-021, REQ-022, REQ-023, REQ-024, REQ-025, REQ-026, REQ-027, REQ-028, REQ-029, REQ-030, REQ-031, REQ-032, REQ-033, REQ-034, REQ-035, REQ-036, REQ-037, REQ-038, REQ-039, REQ-040, REQ-041, REQ-042, REQ-043, REQ-044, REQ-045, REQ-046, REQ-047, REQ-048
- Dependencies: Step 10 已验证并提交
- Baseline state: 前置Step合同已GREEN并提交；本Step缺口：可加载旧产物的真实export/login/logout/stepUp契约断言失败；包未安装或tsc编译错误只算环境阻塞，不能作为RED
- Observable outcome: 校验自身前端制品并完成范围与回归交付门禁；以本Step命名真实行为断言证明，错误分支和负作用同时核对。
- End state: 两个真实安装产物的普通typecheck/test与既有锁定约定一致；受控回归和源范围/SQL不变有记录；Runtime unverified另列。
- Test-first gate: Required — 可加载旧产物的真实export/login/logout/stepUp契约断言失败；包未安装或tsc编译错误只算环境阻塞，不能作为RED；按File顺序签名编译前置后才运行RED，编译/fixture/环境失败不计。
- Manual Checks: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001, MC-REUSE-001, MC-DEP-001；按每File Standards impact及本Step验证分别闭合。
- Literal Rules: Rule 6, Rule 7, Rule 11。
- Ordered files:

#### File 1 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/AuthArtifactContract.test.ts`

- Purpose: 校准旧产物后用真实包验证接口，非mock共享SDK；本次只完成本Step所属切片。
- Symbols: 实际installed接口检查；AuthArtifactContract.test。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，SW AuthContext.tsx/JW gatewayAuth.ts及其现有suite；普通npm test/typecheck解析安装包是现成邻近能力。
- Dependencies and consumers: SW AuthContext.tsx/JW gatewayAuth.ts及其现有suite；普通npm test/typecheck解析安装包；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 真实安装包契约Vitest，import实际@egon-cola/xingyuan-admin-web-shared；不mock该共享模块。
- Input/output and state mapping: createGatewayAuthClient({baseUrl,fetch:mockFetch,navigate:spy})→login()/stepUp跳转；logout先CSRF后POST，remoteState CONFIRMED/PENDING/UNKNOWN。
- Error and edge behavior: fetch仅测试double、no浏览器；缺产物/模块收集失败不是行为RED；测试运行时export/返回契约assert可复现可加载旧包差异；禁source tsconfig alias。
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```typescript
import * as installedShared from "@egon-cola/xingyuan-admin-web-shared";
it("installed_gateway_contract", async () => { expect(installedShared.createGatewayAuthClient).toBeTypeOf("function");
    client=installedShared.createGatewayAuthClient({baseUrl:"https://edge.example",fetch:mockFetch,navigate:spy}); client.login(); client.stepUp();
    expect(spy).包含/oauth2/authorization/edge-ui与?stepUp=true；expect(await client.logout()).toEqual({remoteState:"CONFIRMED"});
    mock 503 remoteState PENDING/UNKNOWN按原wire各断言；所有fetch无password/token body;
});
```

- Verification contribution: 本Step `AuthArtifactContract.test.ts / 普通两个typecheck与原全suite`；fetch仅测试double、no浏览器；缺产物/模块收集失败不是行为RED；测试运行时export/返回契约assert可复现可加载旧包差异；禁source tsconfig alias。 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 2 — `CREATE egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/AuthArtifactContract.test.ts`

- Purpose: 保留声明/不制造新lock，只验证实际包无密码login/logout remoteState/stepUp；本次只完成本Step所属切片。
- Symbols: 现有npm alias真实接口检查；AuthArtifactContract.test。
- Repository evidence: 此完整路径当前不存在；模块POM和主目标§8.3确定owner，SW AuthContext.tsx/JW gatewayAuth.ts及其现有suite；普通npm test/typecheck解析安装包是现成邻近能力。
- Dependencies and consumers: SW AuthContext.tsx/JW gatewayAuth.ts及其现有suite；普通npm test/typecheck解析安装包；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；此测试在行为实现之前固定本Step缺口。
- Contract/signature changes: 真实安装包契约Vitest，import实际@egon-cola/xingyuan-admin-web-shared；不mock该共享模块。
- Input/output and state mapping: createGatewayAuthClient({baseUrl,fetch:mockFetch,navigate:spy})→login()/stepUp跳转；logout先CSRF后POST，remoteState CONFIRMED/PENDING/UNKNOWN。
- Error and edge behavior: fetch仅测试double、no浏览器；缺产物/模块收集失败不是行为RED；测试运行时export/返回契约assert可复现可加载旧包差异；禁source tsconfig alias。
- Standards impact: MC-VALID-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001：按真实入口断言schema/敏感输出与负作用；测试仅JUnit/Vitest原工具链，非Spring生产Bean，模型/DI规范由被测目标证明。
- Literal rule enforcement: Rule 2, Rule 6, Rule 10, Rule 11：实际Validator/JSON/Clock行为断言，测试位于自身模块；TS测试无Java对象/注入，日期仅原wire。
- Implementation pseudocode:

```typescript
import * as installedShared from "@egon-cola/xingyuan-admin-web-shared";
it("installed_gateway_contract", async () => { expect(installedShared.createGatewayAuthClient).toBeTypeOf("function");
    client=installedShared.createGatewayAuthClient({baseUrl:"https://edge.example",fetch:mockFetch,navigate:spy}); client.login(); client.stepUp();
    expect(spy).包含/oauth2/authorization/edge-ui与?stepUp=true；expect(await client.logout()).toEqual({remoteState:"CONFIRMED"});
    mock 503 remoteState PENDING/UNKNOWN按原wire各断言；所有fetch无password/token body;
});
```

- Verification contribution: 本Step `AuthArtifactContract.test.ts / 普通两个typecheck与原全suite`；fetch仅测试double、no浏览器；缺产物/模块收集失败不是行为RED；测试运行时export/返回契约assert可复现可加载旧包差异；禁source tsconfig alias。 的真实断言由本Step测试观察。
- After this file: 此切片可用；行为断言先执行RED，紧随生产File使其GREEN。

#### File 3 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/package-lock.json`

- Purpose: 匹配当前0.2.5产物；无新package/共享源/发布；本次只完成本Step所属切片。
- Symbols: 已声明共享包解析；package-lock。
- Repository evidence: 当前路径存在，已有已声明共享包解析；保留相邻注解/工厂/原异常风格。
- Dependencies and consumers: SW已有package-lock.json与package.json；真实安装包；JW已有npm alias无lock约定；编译/Maven owner是路径中的模块，generated Mapper仅由既有processor生成，不提交target。
- Why now: 先前契约/RED已限定行为；依本Step之前File的精确类型接通该边界，不先启用未完成能力。
- Contract/signature changes: 只校准SW现有共享依赖为已在JW使用的npm alias `npm:@egon-cola/admin-web-shared@^0.2.0`，原逻辑import/key与semver范围不变；package-lock resolution/integrity与可信0.2.5制品一致。
- Input/output and state mapping: 保留package.json/source-import逻辑名；resolved/integrity/实际package name来自可信既有制品元数据；其他依赖项不变。
- Error and edge behavior: version alone不证明源码一致；没有匹配artifact时该Step Execution blocked，不能伪造integrity/手改node_modules源码/发布npm；JW保留alias且无lock。
- Standards impact: MC-CONFIG-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001：配置/lock是数据文件，Java POJO/日志注解不适用；保持同构/真实制品校验和当前owner，无secret值。
- Literal rule enforcement: Rule 7, Rule 11：同核心key或既有artifact声明不变，范围仅本平台；Rule 6：保留JSON/YAML既有字段和无敏感制品输出。
- Implementation pseudocode:

```text
用户PC-014授权：按既有流程构建并发布0.2.5，再registry获取安装至两个自身host。先核对registry name/version/dist integrity/signature和实际认证contract。
校准lock前记录实际package.json、resolved与SHA512 integrity（原artifact字节），logical package入口和包本身name可以因既有别名不同；以三项真实认证契约证明源码对应。
按既有npm lock格式仅更新共享包节点及其必要元数据；审查root依赖range不变、其他包diff=0。
断言realpath安装package name/version和三项真实认证接口；npm普通test/typecheck通过才接受此lock。
若npm解析无法在现有range/name下对应同一artifact，停止该Step报告，不更改包命名或共享源码。
```

- Verification contribution: 本Step `AuthArtifactContract.test.ts / 普通两个typecheck与原全suite`；version alone不证明源码一致；没有匹配artifact时该Step Execution blocked，不能伪造integrity/手改node_modules源码/发布npm；JW保留alias且无lock。 的真实断言由本Step测试观察。
- After this file: 此切片可用；本Step后续wiring和focused GREEN闭合才提交；其他尚未启用能力依End state。


#### File 4 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/package.json`

- Purpose: PC-012用户批准获取安装后的必要解析校准；使用鉴神现有npm alias。
- Symbols: dependencies.@egon-cola/xingyuan-admin-web-shared。
- Repository evidence: npmjs实际包名admin-web-shared@0.2.5；逻辑xingyuan名404；JW已使用同一alias。
- Dependencies and consumers: SW常规npm/tsc/Vitest，源码import保持，旧range ^0.2.0保持。
- Why now: 可信制品已确认，lock必须与root声明一致才能常规解析。
- Contract/signature changes: 唯一dependency值改为npm:@egon-cola/admin-web-shared@^0.2.0；不新增包/版本范围/源码alias。
- Input/output and state mapping: 同一logical key→registry既有实际包→验签完整性/installed0.2.5→常规contract测试。
- Error and edge behavior: 非npmjs/不匹配integrity或0.2.5则停止；不伪造metadata、不更新其他依赖。
- Standards impact: MC-DEP-001,MC-CONFIG-001,MC-SCOPE-001,MC-TEST-001；实际制品身份核对。
- Literal rule enforcement: Rule 6,Rule 7 JSON lock/root一致；Rule11仅两个自身host。
- Implementation pseudocode:

```text
读取SW原dependency key和^0.2.0范围；读取JW既有npm alias机制；
registry实际name=admin-web-shared且version=0.2.5，核对SHA512原tarball；
仅替换SW dependency值npm:@egon-cola/admin-web-shared@^0.2.0；
更新SW lock的root单键和共享节点（仅trusted metadata）；
按既有npm --legacy-peer-deps流程精确安装0.2.5，禁止安装脚本/发布；
JW --no-save --package-lock=false保持原alias与无lock惯例；
运行两host普通typecheck/test与真实installed契约再提交。
```

- Verification contribution: 两套真实installed contract/普通typecheck/fullsuite和lock与tarball对照。
- After this file: 两host依赖身份闭合，完整Step11门禁通过后一次提交。


#### File 5 — `MODIFY egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/package.json`

- Purpose: 用户确认真实npm名rbac3-react-sdk后修复JW既有依赖解析，不引入新SDK或升级版本。
- Symbols: dependencies.@egon-cola/tianquan-jianshen-react-sdk。
- Repository evidence: 原逻辑包名registry404，用户提供实际rbac3-react-sdk详情链接，registry0.1.1与tarball已确认。
- Dependencies and consumers: JW原react SDK进口/页面及普通npm/typecheck/Vitest；shared源码不动。
- Why now: 常规安装不能越过现有404；必要直接依赖必须在本Step闭合。
- Contract/signature changes: 唯一dependency值npm:@egon-cola/rbac3-react-sdk@0.1.1，逻辑key/import与精确版本不变。
- Input/output and state mapping: registry身份/签名/完整性→原SDK制品→JW本host实际安装；无source aliases。
- Error and edge behavior: 不改SDK源或发布SDK；不是用版本号代替认证contract证明；auth0.2.5仍独立验证。
- Standards impact: MC-DEP-001,MC-CONFIG-001,MC-SCOPE-001,MC-TEST-001；解析修正保留原依赖。
- Literal rule enforcement: Rule 6, Rule 7 JSON声明一致；Rule 11仅JW自身，Java规则不涉及。
- Implementation pseudocode:

```text
读取JW原逻辑key和精确0.1.1；
核对用户提供rbac3-react-sdk registry name/version/tarball/integrity/signature；
仅替换此值为npm:@egon-cola/rbac3-react-sdk@0.1.1；
按原npm --legacy-peer-deps安装，ignore-scripts、不新增JW lock；
确认TypeScript实际解析到JW own node_modules，不走parent旧包；
auth0.2.5的OIDC/CSRF/logoutState契约仍需全部通过，否则Step保持Blocked；
所有五路径门禁GREEN后一次提交，不提交失败Step。
```

- Verification contribution: 真实SDK版本/字节核对、常规JW安装及typecheck/contract测试。
- After this file: 404解析修正闭合，正式Step11验收由认证契约和完整门禁决定。


#### File 6 — `MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/package.json`

- Purpose: 用户2026-10-11明确授权构建发布，实际发布名admin-web-shared与精确0.2.5版本，原源码及scripts/peer依赖不改。
- Symbols: 当前publisher元数据/既有release流程。
- Repository evidence: 原name=xingyuan-admin-web-shared、version0.2.4，源码已有新OIDC契约，旧registry制品未包含。
- Dependencies and consumers: 原npm/Vite/tsc/Vitest/registry流程，仅本次publisher与两个自身host。
- Why now: 无正确发布制品不能让Step11真实安装契约GREEN。
- Contract/signature changes: 无认证源码修改；发布元数据name/version修正，消费者继续原logical import及npm alias。
- Input/output and state mapping: 已批准源码→源suite/typecheck→原build/prebuild/postbuild→精确tarball→prepublish两host验证→npmjs0.2.5→registry签名/字节核验。
- Error and edge behavior: E401/EOTP/auth不可用不发布成功；不改/输出token、不自动浏览器登录、不重发已存在同版本、不升级其他库。
- Standards impact: MC-DEP-001,MC-CONFIG-001,MC-SCOPE-001,MC-TEST-001；具名精确release、元数据与tarball一致。
- Literal rule enforcement: Rule 6,Rule 7 JSON/protocol与发布元数据一致；Rule 11原模块、无其他平台接入；Java-only N/A。
- Implementation pseudocode:

```text
核对publisher原元数据与用户精确name/version授权；
将发布名admin-web-shared与0.2.5写入仅必要root元数据，lock其余节点保持；
复用原脚本跑typecheck/源suite/build，不启动服务；
npm pack生成精确tarball，验证files/sha512/新认证声明/实际行为；
两host先安装此tarball做常规contract/typecheck/fullsuite预发布验证；
身份可用时npm publish该已验证tarball --access public --registry=官方registry；
npmjs取回同版本metadata/tarball，核验签名和逐字节一致并精确安装；
八路径Step11完整GREEN后一次提交；身份/输入失败保持Blocked。
```

- Verification contribution: publisher源suite/typecheck/build、两host真实已安装artifact tests/typecheck/fullsuite、registry一致性。
- After this file: 完整发布/安装与全部Step门禁验证后提交；不把本地pack当registry发布成功。


#### File 7 — `MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/package-lock.json`

- Purpose: 用户2026-10-11明确授权构建发布，仅root/top层name/version与publisher一致，不升级依赖节点。
- Symbols: 当前publisher元数据/既有release流程。
- Repository evidence: 原name=xingyuan-admin-web-shared、version0.2.4，源码已有新OIDC契约，旧registry制品未包含。
- Dependencies and consumers: 原npm/Vite/tsc/Vitest/registry流程，仅本次publisher与两个自身host。
- Why now: 无正确发布制品不能让Step11真实安装契约GREEN。
- Contract/signature changes: 无认证源码修改；发布元数据name/version修正，消费者继续原logical import及npm alias。
- Input/output and state mapping: 已批准源码→源suite/typecheck→原build/prebuild/postbuild→精确tarball→prepublish两host验证→npmjs0.2.5→registry签名/字节核验。
- Error and edge behavior: E401/EOTP/auth不可用不发布成功；不改/输出token、不自动浏览器登录、不重发已存在同版本、不升级其他库。
- Standards impact: MC-DEP-001,MC-CONFIG-001,MC-SCOPE-001,MC-TEST-001；具名精确release、元数据与tarball一致。
- Literal rule enforcement: Rule 6,Rule 7 JSON/protocol与发布元数据一致；Rule 11原模块、无其他平台接入；Java-only N/A。
- Implementation pseudocode:

```text
核对publisher原元数据与用户精确name/version授权；
将发布名admin-web-shared与0.2.5写入仅必要root元数据，lock其余节点保持；
复用原脚本跑typecheck/源suite/build，不启动服务；
npm pack生成精确tarball，验证files/sha512/新认证声明/实际行为；
两host先安装此tarball做常规contract/typecheck/fullsuite预发布验证；
身份可用时npm publish该已验证tarball --access public --registry=官方registry；
npmjs取回同版本metadata/tarball，核验签名和逐字节一致并精确安装；
八路径Step11完整GREEN后一次提交；身份/输入失败保持Blocked。
```

- Verification contribution: publisher源suite/typecheck/build、两host真实已安装artifact tests/typecheck/fullsuite、registry一致性。
- After this file: 完整发布/安装与全部Step门禁验证后提交；不把本地pack当registry发布成功。


#### File 8 — `MODIFY egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/README.md`

- Purpose: 用户2026-10-11明确授权构建发布，记录实际发布身份、既有npm alias及此次仅两个host接入，保留既有流程。
- Symbols: 当前publisher元数据/既有release流程。
- Repository evidence: 原name=xingyuan-admin-web-shared、version0.2.4，源码已有新OIDC契约，旧registry制品未包含。
- Dependencies and consumers: 原npm/Vite/tsc/Vitest/registry流程，仅本次publisher与两个自身host。
- Why now: 无正确发布制品不能让Step11真实安装契约GREEN。
- Contract/signature changes: 无认证源码修改；发布元数据name/version修正，消费者继续原logical import及npm alias。
- Input/output and state mapping: 已批准源码→源suite/typecheck→原build/prebuild/postbuild→精确tarball→prepublish两host验证→npmjs0.2.5→registry签名/字节核验。
- Error and edge behavior: E401/EOTP/auth不可用不发布成功；不改/输出token、不自动浏览器登录、不重发已存在同版本、不升级其他库。
- Standards impact: MC-DEP-001,MC-CONFIG-001,MC-SCOPE-001,MC-TEST-001；具名精确release、元数据与tarball一致。
- Literal rule enforcement: Rule 6,Rule 7 JSON/protocol与发布元数据一致；Rule 11原模块、无其他平台接入；Java-only N/A。
- Implementation pseudocode:

```text
核对publisher原元数据与用户精确name/version授权；
将发布名admin-web-shared与0.2.5写入仅必要root元数据，lock其余节点保持；
复用原脚本跑typecheck/源suite/build，不启动服务；
npm pack生成精确tarball，验证files/sha512/新认证声明/实际行为；
两host先安装此tarball做常规contract/typecheck/fullsuite预发布验证；
身份可用时npm publish该已验证tarball --access public --registry=官方registry；
npmjs取回同版本metadata/tarball，核验签名和逐字节一致并精确安装；
八路径Step11完整GREEN后一次提交；身份/输入失败保持Blocked。
```

- Verification contribution: publisher源suite/typecheck/build、两host真实已安装artifact tests/typecheck/fullsuite、registry一致性。
- After this file: 完整发布/安装与全部Step门禁验证后提交；不把本地pack当registry发布成功。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`（Maven）；Step11 npm用下列完整 --prefix，无服务进程。
- Verification command: `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run test -- src/auth/AuthArtifactContract.test.ts；npm --prefix egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web run test -- src/features/auth/AuthArtifactContract.test.ts；然后各自 `run typecheck`、`run test`，再执行§8最终选择器与git边界门禁。`；该Step `git diff --check` 与精确commit path审查。
- Expected result: RED必须是上述缺失行为断言；GREEN exit0、目标命名tests实际执行且0 failures/errors/非预期skip；无源schema、Bean注入/敏感值或旧机制退化。Step11普通解析不得走临时alias。
- Failure returns to: 本Step对应assert的生产File；schema/DI到对应载体/factory，receipt状态到Step3/4，原合同退化停止当前Step；artifact缺失停止Step11等待可信输入；若需加库/改SQL/外platform或改Spec语义，回用户/Spec。
- Completion criteria: 两个真实安装产物的普通typecheck/test与既有锁定约定一致；受控回归和源范围/SQL不变有记录；Runtime unverified另列；保存实际RED/GREEN命令、测试report/准确数量、适用MC证据及当前HEAD，没有业务未完成签名/placeholder或外部runtime假成功。
- Rollback: 未部署时只还原本Step相对前置HEAD的精确diff，保留之前Step及未归属改动；已产生审计/spool时禁止删帧/回旧consumer，按§9停止入口并forward-fix。
- Commit paths: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/AuthArtifactContract.test.ts`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/AuthArtifactContract.test.ts`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/package-lock.json`, `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/package.json`, `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/package.json`, `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/package.json`, `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/package-lock.json`, `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/README.md`.
- Commit: `test(tianquan): verify own auth artifacts and exit gates`；本轮只规划，不执行。


## 8. Test, Validation, and Quality Gates

以下均为执行阶段的未来门禁，本轮只实际运行文档validator/库存/链接与diff检查。所有mvn命令必须离线、串行、指定测试；不要无guard运行全reactor或激活本地integration profile。新fixture只用temp目录、受控Clock/Redisson接口double、原安全Web slice/实际MapStruct、真实RSA与SimpleMeterRegistry；不能用mock整个PDP/手调record代替真实执行点。

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| RED/GREEN Step 1 | `/Users/mario/SelfProject/Egon-COLA` | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=OAuthNativeCorsContractTest,OAuthProtocolOpenApiContractTest,IdpPublishedOpenApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | OAuthNativeCorsContractTest,OAuthProtocolOpenApiContractTest,IdpPublishedOpenApiContractTest | 先指定行为RED，再exit0且report中目标test存在/无非预期skip | 对应File；环境失败不计RED | REQ-001,REQ-002,REQ-003,REQ-014,REQ-016,REQ-018；受控，无外部DB/Redis |
| RED/GREEN Step 2 | `/Users/mario/SelfProject/Egon-COLA` | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter -am -Dtest=Rbac3FieldWriteAuthorizationComponentTest,DefaultAuthorizationServiceTest,Rbac3FieldPropertyWriterTest,Rbac3MethodAuthorizationManagerTest -Dsurefire.failIfNoSpecifiedTests=false test` | Rbac3FieldWriteAuthorizationComponentTest,DefaultAuthorizationServiceTest,Rbac3FieldPropertyWriterTest,Rbac3MethodAuthorizationManagerTest | 先指定行为RED，再exit0且report中目标test存在/无非预期skip | 对应File；环境失败不计RED | REQ-001,REQ-002,REQ-012,REQ-013,REQ-014,REQ-018；受控，无外部DB/Redis |
| RED/GREEN Step 3 | `/Users/mario/SelfProject/Egon-COLA` | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=TokenUsageAuditComponentTest -Dsurefire.failIfNoSpecifiedTests=false test` | TokenUsageAuditComponentTest | 先指定行为RED，再exit0且report中目标test存在/无非预期skip | 对应File；环境失败不计RED | REQ-001,REQ-006,REQ-008,REQ-014,REQ-018；受控，无外部DB/Redis |
| RED/GREEN Step 4 | `/Users/mario/SelfProject/Egon-COLA` | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TokenUsageAuditComponentTest,OAuthAuditConsumerTest,TokenUsageAuditReceiptContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | TokenUsageAuditComponentTest,OAuthAuditConsumerTest,TokenUsageAuditReceiptContractTest | 先指定行为RED，再exit0且report中目标test存在/无非预期skip | 对应File；环境失败不计RED | REQ-001,REQ-002,REQ-006,REQ-007,REQ-008,REQ-014,REQ-015,REQ-018；受控，无外部DB/Redis |
| RED/GREEN Step 5 | `/Users/mario/SelfProject/Egon-COLA` | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest -Dsurefire.failIfNoSpecifiedTests=false test` | MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest | 先指定行为RED，再exit0且report中目标test存在/无非预期skip | 对应File；环境失败不计RED | REQ-001,REQ-002,REQ-009,REQ-010,REQ-011,REQ-014,REQ-018；受控，无外部DB/Redis |
| RED/GREEN Step 6 | `/Users/mario/SelfProject/Egon-COLA` | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am -Dtest=IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test` | IdpSecurityObservationFilterTest,MultiIssuerResourceServerTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest | 先指定行为RED，再exit0且report中目标test存在/无非预期skip | 对应File；环境失败不计RED | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-015,REQ-016,REQ-018；受控，无外部DB/Redis |
| RED/GREEN Step 7 | `/Users/mario/SelfProject/Egon-COLA` | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am -Dtest=TianquanSecurityMetricsContractTest,IdpStarterAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test` | TianquanSecurityMetricsContractTest,IdpStarterAutoConfigurationTest | 先指定行为RED，再exit0且report中目标test存在/无非预期skip | 对应File；环境失败不计RED | REQ-001,REQ-009,REQ-010,REQ-011,REQ-014,REQ-015,REQ-018；受控，无外部DB/Redis |
| RED/GREEN Step 8 | `/Users/mario/SelfProject/Egon-COLA` | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am -Dtest=TianquanSecurityMetricsContractTest,OAuthConsentRoundTripTest,OAuthOidcProtocolContractTest,OAuthAuthorizationPersistenceContractTest,OAuthTokenClientCredentialsTest,OAuthProtocolOpenApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | TianquanSecurityMetricsContractTest,OAuthConsentRoundTripTest,OAuthOidcProtocolContractTest,OAuthAuthorizationPersistenceContractTest,OAuthTokenClientCredentialsTest,OAuthProtocolOpenApiContractTest | 先指定行为RED，再exit0且report中目标test存在/无非预期skip | 对应File；环境失败不计RED | REQ-001,REQ-002,REQ-004,REQ-005,REQ-006,REQ-009,REQ-014,REQ-018；受控，无外部DB/Redis |
| RED/GREEN Step 9 | `/Users/mario/SelfProject/Egon-COLA` | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am -Dtest=Rbac3AdminSecurityChainTest,TianquanSecurityMetricsContractTest,Rbac3AdminSecurityConfigurationTest,Rbac3FieldWriteAuthorizationComponentTest,Rbac3MethodAuthorizationManagerTest -Dsurefire.failIfNoSpecifiedTests=false test` | Rbac3AdminSecurityChainTest,TianquanSecurityMetricsContractTest,Rbac3AdminSecurityConfigurationTest,Rbac3FieldWriteAuthorizationComponentTest,Rbac3MethodAuthorizationManagerTest | 先指定行为RED，再exit0且report中目标test存在/无非预期skip | 对应File；环境失败不计RED | REQ-001,REQ-002,REQ-004,REQ-005,REQ-011,REQ-012,REQ-014,REQ-015,REQ-018；受控，无外部DB/Redis |
| RED/GREEN Step 10 | `/Users/mario/SelfProject/Egon-COLA` | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am -Dtest=TianquanSecurityMetricsContractTest,Rbac3AdminSecurityChainTest,IdpStarterAutoConfigurationTest,SpringConstructorResolutionTest -Dsurefire.failIfNoSpecifiedTests=false test` | TianquanSecurityMetricsContractTest,Rbac3AdminSecurityChainTest,IdpStarterAutoConfigurationTest,SpringConstructorResolutionTest | 先指定行为RED，再exit0且report中目标test存在/无非预期skip | 对应File；环境失败不计RED | REQ-001,REQ-002,REQ-006,REQ-014,REQ-015,REQ-016,REQ-018,REQ-019；受控，无外部DB/Redis |
| RED/GREEN Step 11 | `/Users/mario/SelfProject/Egon-COLA` | 本Step普通npm --prefix命令，详见§7 | 两个实际installed artifact | 先指定行为RED，再exit0且report中目标test存在/无非预期skip | 对应File；环境失败不计RED | REQ-001,REQ-002,REQ-014,REQ-017,REQ-018,REQ-019；受控，无外部DB/Redis |
| 最终受影响模块/跨模块回归 | `/Users/mario/SelfProject/Egon-COLA` | `mvn -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin,egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am -Dtest=OAuthNativeCorsContractTest,OAuthProtocolOpenApiContractTest,IdpPublishedOpenApiContractTest,IdpSecurityObservationFilterTest,TokenUsageAuditComponentTest,OAuthAuditConsumerTest,TokenUsageAuditReceiptContractTest,Rbac3FieldWriteAuthorizationComponentTest,Rbac3AdminSecurityChainTest,TianquanSecurityMetricsContractTest,MultiIssuerResourceServerTest,IdpServiceOAuth2ClientTest,IdpRpcBearerServerInterceptorTest,IdpStarterAutoConfigurationTest,Rbac3AdminSecurityConfigurationTest,DefaultAuthorizationServiceTest,Rbac3FieldPropertyWriterTest,Rbac3MethodAuthorizationManagerTest,AdminLayerBoundaryTest,OAuthConsentRoundTripTest,OAuthOidcProtocolContractTest,OAuthAuthorizationPersistenceContractTest,OAuthTokenClientCredentialsTest,TenantIssuerIsolationTest,SpringConstructorResolutionTest,ClientResourceGrantControllerTest,ResourceServerPolicyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 新行为+原native/C10/issuer/撤销/tenant/method/field/cache/架构/Bean | exit0，命名类在各owner report真实执行，0 failure/error/非预期skip；不以-am命名依赖未运行解释成全仓库PASS | 按失败归属Step；原组件baseline失败单独报 | REQ-002/014/019；受控模块边界 |
| 前端常规run typecheck | `/Users/mario/SelfProject/Egon-COLA` | `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run typecheck` | 原suite+真实共享artifact contract | exit0、无source alias、actual artifact已核对；UI/runtime不包含 | Step11／可信artifact输入 | REQ-017/019；Vitest/tsc，不启Vite/browser |
| 前端常规run test | `/Users/mario/SelfProject/Egon-COLA` | `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web run test` | 原suite+真实共享artifact contract | exit0、无source alias、actual artifact已核对；UI/runtime不包含 | Step11／可信artifact输入 | REQ-017/019；Vitest/tsc，不启Vite/browser |
| 前端常规run typecheck | `/Users/mario/SelfProject/Egon-COLA` | `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web run typecheck` | 原suite+真实共享artifact contract | exit0、无source alias、actual artifact已核对；UI/runtime不包含 | Step11／可信artifact输入 | REQ-017/019；Vitest/tsc，不启Vite/browser |
| 前端常规run test | `/Users/mario/SelfProject/Egon-COLA` | `npm --prefix egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web run test` | 原suite+真实共享artifact contract | exit0、无source alias、actual artifact已核对；UI/runtime不包含 | Step11／可信artifact输入 | REQ-017/019；Vitest/tsc，不启Vite/browser |
| SQL与构建/其他platform不可变 | `/Users/mario/SelfProject/Egon-COLA` | `git diff --exit-code 3a82ea27652232c741b3366e94da8b3b36ea03bd -- "**/src/main/resources/db/**" "**/repository-manifest.json" "**/pom.xml" ":(exclude)egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-contract/pom.xml" ":(exclude)egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/pom.xml" ":(exclude)egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/pom.xml" "**/package.json" egon-cola-archetypes egon-cola-xingyuan/egon-cola-yuheng egon-cola-xingyuan/egon-cola-tianshu egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared` | 历史SQL/manifest、POM/声明、模板/其他platform/shared source | exit0；若之后别任务改同paths，先用已保存pre-Step归属snapshot隔离，不能还原他人工作 | 原批准scope/用户 | REQ-001/018；静态字节差异 |
| 文件范围/格式/敏感内容 | `/Users/mario/SelfProject/Egon-COLA` | `git diff --check`；`git diff --name-only 3a82ea27652232c741b3366e94da8b3b36ea03bd`；`git status --short`；`git diff --cached --stat`逐项与§5/本Step Commit paths核对；Java改动检查禁止新Date/Calendar/SimpleDateFormat、Gson/Fastjson/ordinal/手工mapper与token日志 | 每次提交前及最终 | 只有列出paths；所有strict/model/annotation/factory代码由命名测试覆盖；无secret和generated target进入commit | 对应File；发现范围扩张先停 | REQ-014/018；静态/审阅 |
普通mvn上游safe选择器不运行Common Cache/MP全suite的既有外部fixtures。历史报告记录过无guard测试启动容器及Common Cache/MP baseline失败，不能称已解决；没有新变更不重复全仓库测试。若选择器解析导致目标0 tests，必须直接阻断，即使Surefire因上游failIfNoSpecifiedTests=false退出0。
密码学埋点依据：本地Boot3.5.16 BOM管理Security6.5.11，现缓存源码/JAR与其Nimbus9.37.4声明已复核；NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder.jwtProcessorCustomizer、ConfigurableJWTProcessor.get/setJWSVerifierFactory与JWSVerifier.verify均真实存在。只包装真正verify调用；authenticate/decoder总耗时不能冒充密码学耗时。SDK改原manager builder，SAS改既有DefaultJWTProcessor，均保留原provider/algorithm/key选择，不加依赖或第二decoder。
无需新的scratch harness：JUnit/Vitest新增已批准测试路径覆盖行为、schema/parity/DI、signature与state；现有git diff/status/文档validator覆盖路径和字节不变门禁。scripts/work新增文件为N/A，既有ignored资料不碰；本轮用于写文档的/tmp临时生成/校验文件不属于仓库验证harness或commit。

### 用户控制的运行验收

SELF-RUNTIME保留为单独授权门禁：用户自行启动指定环境后，核验两个租户相互拒绝、DB commit/重复/unknown提交与180天行为、Redis PRIMARY failover/receipt PEL/丢TTL、两个真实producer独占卷重启/掉电/满容量/DEAD隔离、真实TLS证书/ACL与key一致、两个自身浏览器Code/PKCE/CSRF/登录退出、实际JWT timer/cache信号与负载p95。未提供或未执行时一律Runtime unverified，既不阻碍Plan Review，也不把受控GREEN推成生产Ready。此Plan不含自动启动服务/infra或浏览器的命令。

## 9. Migration, Compatibility, Rollout, and Rollback

关系数据库迁移、DDL版本/manifest、表/索引/UK、数据回填、codegen/DDL consumption journal均N/A：主目标§11明确无关系模型改动，旧PO/query/SQL字节保持；原MP-SDJ分布式DDL/Outbox同源事务继续有效，任何已有Flyway不编辑。若真实实施发现必须改变这些事实，回Spec与用户，不以本Plan授权修schema或全域JPA。
部署顺序固定：先构建并验证Schema1 consumer/receipt兼容能力→用户确认consumer版本、producer独占持久卷/原audit key/ACL与HTTPS inputs→仅两个自身Admin启usage observation/指标→受控回归与授权runtime→用户审核SELF交付后才另做OTHER-INTEGRATION。不能先让新producer发送再升级consumer；旧unsigned Outbox仍原路径，签名present的usage无tokenRef也验MAC。
滚动兼容：旧Event11字段/frame可读、旧API/Bean名/构造与两typed final wrapper/Valid JSON不变。新receipt缺失/过期只导致同id重放和spool保留；新DEAD留隔离且计容量，EXPIRED只用于既有180天规则、不重建已purge行。Consumer只写receipt、sender只读并清自己的已确认目录；原schema与secret references不在新消息扩张。
源码回退只撤当前Step相对前置HEAD的精确切片，不恢复已完成Step/他人改动；未产生新事实时可以回旧二进制。自身已启strict审计并形成evt/sent/dead后，先停止新受保护入口，保持新consumer处理并对账，保留所有目录和DB事实；不得回退consumer使原sent永久无法确认，不删帧/降级broker-only放行。已撤销token/key/security generation不可恢复，优先forward-fix。禁自动部署、删spool、修DDL history或重启broker。
SELF-SOURCE=实际范围/编译/schema；SELF-CONTROLLED=命名真实链/双目录/field/metrics+普通前端GREEN；SELF-RUNTIME=用户授权真实环境证据；OTHER-INTEGRATION=用户另行决定范围与Spec/Plan。当前只提供Plan，不替任何一层宣称已执行。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| REQ-001 | 主目标§4；对应§7–16 | Step 1,Step 2,Step 3,Step 4,Step 5,Step 6,Step 7,Step 8,Step 9,Step 10,Step 11 | §5精确库存及各Step Commit paths | TEST-006/028；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-002 | 主目标§4；对应§7–16 | Step 1,Step 2,Step 4,Step 5,Step 6,Step 8,Step 9,Step 10,Step 11 | §5精确库存及各Step Commit paths | TEST-003/021/025；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-003 | 主目标§4；对应§7–16 | Step 1 | §5精确库存及各Step Commit paths | TEST-001–004；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-004 | 主目标§4；对应§7–16 | Step 6,Step 8,Step 9 | §5精确库存及各Step Commit paths | TEST-007/008；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-005 | 主目标§4；对应§7–16 | Step 6,Step 8,Step 9 | §5精确库存及各Step Commit paths | TEST-005/008；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-006 | 主目标§4；对应§7–16 | Step 3,Step 4,Step 6,Step 8,Step 10 | §5精确库存及各Step Commit paths | TEST-009/010；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-007 | 主目标§4；对应§7–16 | Step 4 | §5精确库存及各Step Commit paths | TEST-011–014；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-008 | 主目标§4；对应§7–16 | Step 3,Step 4 | §5精确库存及各Step Commit paths | TEST-010/013/015/016；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-009 | 主目标§4；对应§7–16 | Step 5,Step 6,Step 7,Step 8 | §5精确库存及各Step Commit paths | TEST-022；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-010 | 主目标§4；对应§7–16 | Step 5,Step 7 | §5精确库存及各Step Commit paths | TEST-023；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-011 | 主目标§4；对应§7–16 | Step 5,Step 7,Step 9 | §5精确库存及各Step Commit paths | TEST-024；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-012 | 主目标§4；对应§7–16 | Step 2,Step 9 | §5精确库存及各Step Commit paths | TEST-017–021；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-013 | 主目标§4；对应§7–16 | Step 2 | §5精确库存及各Step Commit paths | TEST-018/019；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-014 | 主目标§4；对应§7–16 | Step 1,Step 2,Step 3,Step 4,Step 5,Step 6,Step 7,Step 8,Step 9,Step 10,Step 11 | §5精确库存及各Step Commit paths | TEST-004/014/017/026；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-015 | 主目标§4；对应§7–16 | Step 4,Step 6,Step 7,Step 9,Step 10 | §5精确库存及各Step Commit paths | TEST-006/026；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-016 | 主目标§4；对应§7–16 | Step 1,Step 6,Step 10 | §5精确库存及各Step Commit paths | TEST-003/008/026/028；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-017 | 主目标§4；对应§7–16 | Step 11 | §5精确库存及各Step Commit paths | TEST-027；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-018 | 主目标§4；对应§7–16 | Step 1,Step 2,Step 3,Step 4,Step 5,Step 6,Step 7,Step 8,Step 9,Step 10,Step 11 | §5精确库存及各Step Commit paths | TEST-012/028；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| REQ-019 | 主目标§4；对应§7–16 | Step 10,Step 11 | §5精确库存及各Step Commit paths | TEST-028；§8命令 | 未来实际执行report/HEAD；Runtime单列，当前仅设计审阅 |
| ALIGN:REQ-020（REQ-020） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-021（REQ-021） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-022（REQ-022） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-023（REQ-023） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-024（REQ-024） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-025（REQ-025） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-026（REQ-026） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-027（REQ-027） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-028（REQ-028） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-029（REQ-029） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-030（REQ-030） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-031（REQ-031） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-032（REQ-032） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-033（REQ-033） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-034（REQ-034） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-035（REQ-035） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-036（REQ-036） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-037（REQ-037） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-038（REQ-038） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-039（REQ-039） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-040（REQ-040） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-041（REQ-041） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-042（REQ-042） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-043（REQ-043） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-044（REQ-044） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-045（REQ-045） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-046（REQ-046） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-047（REQ-047） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
| ALIGN:REQ-048（REQ-048） | [代码修订](../spec/2026-10-08-10-18-oauth-current-code-alignment.md) §4与§2限定段落 | Step11（context-only） | 不新增前置旧任务paths；自身回归使用本Plan库存，其他platform只检查无diff | 原native/C10/Bean/SQLscope与§8；不对未授权外部运行补测 | 合同保留/字节不变，原全局执行和运行缺口不在本轮宣称完成 |
前置OIDC、FIELD、OUTBOX重复REQ标识按§2的文档命名空间解析：保留机制的需求对应主目标REQ-002/014/018与Step1/2/4/6/8/9/11边界回归；不因validator把不同文档的裸REQ并集合并而扩大当前任务。原UserInfo/CSRF/C10以及Outbox技术事务的实际测试选择器已列§8，业务SSM/全域集成保持范围外。

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| DEC-PLAN-001 | 主目标批准、显式field guard、仅自身/只Plan | 全部 | 用户本轮/此前明确问答；主Spec Accepted | mario | Closed；本轮不执行源码 |
| DEC-PLAN-002 | RPC interceptor漏项 | Step6 / 原SS RPC文件 | 原with(principal,token)无法传播Valid.proof；用户已批准补清单 | mario | Closed；Spec已补路径，public old signatures保持 |
| RISK-PLAN-001 | 卷atomic/dir durability、key/Redis ACL/TLS缺真实输入 | Step3/4/10与SELF-RUNTIME | 主目标RISK001–003与§15/16 | 用户/运行维护者 | 已明确执行输入门禁；不可验证则不ready/Runtime unverified，不是未决设计 |
| RISK-PLAN-002 | 真实shared artifact缺失或name/resolution不匹配 | Step11 | SW旧0.2.3、JW alias无安装/无lock；声明/源码已核对 | 用户/制品维护者 | 已确定使用既有0.2.4匹配产物；准备不足则Execution blocked，不能改命名/下载/发布冒充修复 |
| RISK-PLAN-003 | 外平台直接ctor/custom verifier兼容 | Step5/6 | Edge manager旧三参、final USER/SERVICE wrapper；factory与old JSON兼容设计 | 执行者 | 检查旧public消费者；发现需外部变更则回Spec，不突破范围 |
| RISK-PLAN-004 | 同id冲突/unknown commit/receipt丢失/DEAD误删 | Step4 | 原save/findByEventId、原状态与新增signed fingerprint | 执行者 | 测试负例/先persist后receipt后XACK；不声明DB/MQ原子事务 |
| RISK-PLAN-005 | 前置Review metadata/REQ重号误读为全局实施授权 | §2/3/10/11 | 旧Plan执行记录与其Review状态，当前Primary限定依赖 | 执行者/用户 | Closed；只采用点名合同，原Plan运行/广域缺口维持历史事实 |
无未回答的业务权限/模型/新依赖/架构重大选择。制品、部署、授权runtime为明确的未来执行输入，未提供时停止相关执行，不允许以设计PASS替代；Plan尚待用户审核，当前不会自动执行Step1。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

19项主目标REQ都有Step与trace；28项TEST均有明确文件/选择器或用户runtime边界。只两子平台65文件，其他平台/模板接入后续；保留现有最优机制；显式field guard不接业务DTO；原协议/密码/CSRF/tenant/方法/read/key/revoke/SQL不重做。

### 12.2 Spec consistency

无设计重建、fetch-then-forward/API预检、自动字段过滤、新依赖/库/cache/job/表/层/页面；必要性审阅见§4.5。五个Plan Clarification仅Test顺序/Common实例API/final wrapper组合/cache固定recorder等本地实现细节。RPC现有调用点已由用户补充批准并同步Spec，不藏在clarification。

### 12.3 Repository executability

当前基线核对66路径CREATE/存在状态；旧manager ctor、final ports、RPC Context调用、PDP三字段、save/findByEventId、具体tests、POM/npm脚本、模块lombok配置与defaultValidator已复核。每Step有严格File顺序、签名/mapping/state/errors/pseudocode、intermediate RED/GREEN、WD/选择器/commit exact paths；重复路径只有分段技术切片，无stub commit。测试方法是将新建/扩充的明确方法，不能当作当前已存在。

### 12.4 Test and release completeness

每步先RED且编译前置不算行为证据；关键守卫断言业务零调用、证明单次验证、严格存储、原同源commit→receipt→XACK、双目录仅producer安全清理、固定实际signal、schema/Bean/parity与普通artifact解析。SQL/迁移N/A有字节门禁；只受控focused与指定回归，真实运行另列。当前已执行仅技能预检、两文档strict validator、库存/覆盖/link/scope/diff检查，不是Maven/npm/runtime PASS。

### 12.5 Blocking Manual Check

下表PASS只指Plan审阅、仓库依据和未来验证路径已闭合；不指生产实现、单元行为或运行验收已通过。

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | Applicable | PASS | 主目标§6.1；实际两Admin传统树/JA AdminLayerBoundaryTest；§4.7/5 | 唯一传统三层和现有SDK边界；Core不反向依赖host | None；执行时必须按§7/8真实验证 |
| `MC-REUSE-001` | Applicable | PASS | §4.7 ledger：SAS/CORS/Common/MapStruct/Redisson/Actuator/PDP/MP/Outbox | 均复用现有；必要差异单一owner | None；执行时必须按§7/8真实验证 |
| `MC-DEP-001` | Applicable | PASS | 根及模块POM已核对；§6/8仅3个POM已批准编译补齐；package声明与其他POM不改、不下载 | 只批准同版本Validation API/Lombok provided/MapStruct处理器；无新运行框架或下载 | None；执行时必须按§7/8真实验证 |
| `MC-NAME-001` | Applicable | PASS | §5所有BO/DTO/Command/Component/Converter/Properties/port与File Symbols | 角色确切，未新增Data/Info/Param/Bean carrier | None；执行时必须按§7/8真实验证 |
| `MC-VALID-001` | Applicable | PASS | §4.8 Rule2；Step2/4/6/7/10对应schema/groups/default/cascade及TEST008/014/017/026 | 非代理手交接用真实Common实例API；SPI override不强化；本轮无电话模型 | None；执行时必须按§7/8真实验证 |
| `MC-MODEL-001` | Applicable | PASS | Step2/4/5/6载体完整注解与immutable proof/Event/decision；§9无关系模型变化 | 建模/集合/构造语义已定；旧soft-delete UK无变动、不伪造receipt业务表 | None；执行时必须按§7/8真实验证 |
| `MC-CONVERT-001` | Applicable | PASS | Step2 FieldWriteAuthorizationConverter、Step6 TokenUsageAuditEventConverter extends BaseForwardConverter | 两个真实不可逆投影均generated MapStruct，禁止setter/JSON替代 | None；执行时必须按§7/8真实验证 |
| `MC-LOG-001` | Applicable | PASS | 业务File standards+pseudocode与§8敏感内容审阅 | 所有新改业务Slf4j/安全参数化日志，rawToken/secret无输出 | None；执行时必须按§7/8真实验证 |
| `MC-BEAN-001` | Applicable | PASS | Step2 JA lombok.config；原SC/SS/SA config；Step6/7 qualified factory/Supplier与TEST026 | 具名Bean+Lombok Qualified，Boot primary/unique registry无重复 | None；执行时必须按§7/8真实验证 |
| `MC-UTIL-001` | Applicable | PASS | FileChannel/Mac/MessageDigest/JDK URI与Common facade；§8imports检查 | 仅允许既有库，未新增Utils/Tika/JSON栈 | None；执行时必须按§7/8真实验证 |
| `MC-JSON-001` | Applicable | PASS | Step1 OAS、Step4 strict六字段receipt、Step6 JsonIgnore proof/原11字段+TEST004/014/025 | Boot Jackson/wire/sensitive/unknown契约明确，旧enum与JSON保留 | None；执行时必须按§7/8真实验证 |
| `MC-TIME-001` | Applicable | PASS | Step3/4/5/6/7/10 UTC Instant、Duration、Clock与monotonic、TEST016/022/023 | 时间/TTL/expiry/precision及JSON保持，无Date | None；执行时必须按§7/8真实验证 |
| `MC-CONFIG-001` | Applicable | PASS | Step10完整6profile路径、Step4/6 Props与Step7 host、TEST026 | 核心key同构，disabled/offline/local例外明确 | None；执行时必须按§7/8真实验证 |
| `MC-PATTERN-001` | Applicable | PASS | §4.5/4.7；Step4 State、Step6/9 Decorator、Step5/7 Observer Adapter与固定Strategy | Complex变体已有参与者；field集合Simple不制造额外策略层 | None；执行时必须按§7/8真实验证 |
| `MC-SCOPE-001` | Applicable | PASS | §5 66路径与§8不可变gate；本turn只Spec/Plan diff；RPC漏项用户批准 | 没有其他platform/template/shared/SQL或源码编辑副作用 | None；执行时必须按§7/8真实验证 |
| `MC-TEST-001` | Applicable | PASS | §7每Step RED/GREEN/WD/选择器/MC/commit；§8/10 TEST001–028 | 验证可执行且边界诚实；当前只文档验证，不冒充行为GREEN | None；执行时必须按§7/8真实验证 |
| `MC-BLOCKER-001` | Applicable | PASS | §11重大决策已闭合；以上16项证据与执行输入状态 | Plan可供审阅；制品/部署未验证保留门禁，无设计问题待默认猜测 | None；执行时必须按§7/8真实验证 |

### 12.6 Final verdict

**PASS — Ready for user review**

用户已于2026-10-10明确批准按本Plan改造；当前Status为In Progress，依下方逐Step执行记录推进。设计/文档校验通过不代表65文件已实现、Maven/npm已测试、SQL已执行或生产安全达标；SELF-RUNTIME与其他platform/template接入继续受用户控制。

## Execution record

用户明确授权执行Review修订（2026-10-10 14:03 Asia/Shanghai），允许逐Step验证与精确提交；不授权启动服务、DB、browser、部署/push。

### Step 1 lock — In Progress

- 基线：main@3a82ea27652232c741b3366e94da8b3b36ea03bd；无源码/staged重叠，仅本轮Spec/Plan未跟踪文档。
- 范围：章7 Step1列出的七个完整路径，REQ-001/002/003/014/016/018；依赖None。
- RED：/tmp/tq-step1-behavior-red.log、.exit；命名7测试、3 failures、0 errors、0 skip，exit1。四资源返回403而非200、旧退休接口带allow header、OAS缺四OPTIONS。前两次夹具双fallback/丢失handle确认不是RED，调整同测试夹具后以上命令验证。
- 校验命令/rollback/提交message按章7；尚未验证完成，禁止推进Step2。源码仅当前Step。
- 机械修正：同一CorsFilter显式绑定processor（PLAN-CLAR-005）；重复的ConfigurationProperties method注解移除，载体class注解/完整Lombok/Validation保留。
- 门禁将在GREEN后逐Rule及17项MC记录；当前测试门禁未闭合，不提交。

### Step 1 Verified gate

- 最新focused：`/tmp/tq-step1-exact-final.log`/`.exit`：8 tests/0 failure/error/skip，exit0。扩展原SAS Code/PKCE/CSRF/nonce/UserInfo/RT回归：`/tmp/tq-step1-final-green.log`：50 tests/0 failure/error/skip，exit0。使用章7精确Maven命令；扩展只增加OAuthConsentRoundTripTest/OAuthOidcProtocolContractTest。
- 静态：git diff --check exit0；POM/package/SQL/manifest/其他platform未改；没有新scratch仓库脚本。测试只native fixture/temp key与MockMVC，无应用/PG/Redis/browser启动。

| Literal rule | Applicability | Status | Diff/path/symbol evidence | Test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | Properties/Processor/Configuration/Test精确角色；原Bean名显式保留 | 类型源码、编译8 tests | 语义命名 | None |
| Rule 2 | Applicable | PASS | Properties Jakarta容器NotBlank/Size+AssertTrue；factory Common实例defaultValidator；header schema在processor | 5 native tests：schema/路径/header负例 | 所有新边界校验，未添加电话字段 | None |
| Rule 3 | Applicable | PASS | Properties完整@Data/NoArgs/AllArgs/Accessors/Builder与List防御复制 | 编译及真实Validator test | 无record/新converter，根不伪callSuper | None |
| Rule 4 | Applicable | PASS | Processor Slf4j+RequiredArgsConstructor+qualified final delegate；factory具名qualified参数；配置默认Bean名保持 | 真实Spring fixture、javap Constructor Qualifier=oauthDefaultCorsProcessor | 无手写业务DI ctor；技术配置无注入fields | None |
| Rule 5 | Applicable | PASS | JDK URI/List/Map/Set/regex+现Spring/Common；无新Utils/依赖 | POM/DDL/manifest diff=0 | 工具allowlist保持 | None |
| Rule 6 | Applicable | PASS | 四OAS OPTIONS无body/security=[]，原JSON/schema/status族保持；无新JSON库 | OAS2 tests/published1 +原native回归 | Jackson原合同 | None |
| Rule 7 | Applicable | PASS | 仅既有allowed-origins key类型化，默认empty集合；核心profile新键留原Step10 | 未修改任何YAML，env-only local例外默认false | 已有profile键结构不变，不开启后续能力 | None |
| Rule 9 | Applicable | PASS | DefaultCorsProcessor Decorator及限定Spring filter装配；固定资源约束Simple | 真实链非法/合法路径测试；PC005已记录 | 复用模式，无第二CORS框架 | None |
| Rule 10 | Applicable | PASS | native setMaxAge(600L)单位秒；无新日期实体/Date | 时间header断言600与imports检查 | Framework秒单位，java.time规范未削弱 | None |
| Rule 11 | Applicable | PASS | 仅SA原oauth/config/support/security/tests，传统结构 | 七路径精确检查；SQL/POM/manifest无diff | 无COLA新层/其他platform改动 | None |

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 七个SA路径、原SAS/Controller→Service结构 | 结构不变 | None |
| MC-REUSE-001 | Applicable | PASS | DefaultCorsProcessor/CorsFilter/Common ValidationUtils/现Actuator ObservationRegistry | 复用现有能力 | None |
| MC-DEP-001 | Applicable | PASS | POM/package.json零diff，离线Maven | 零依赖引入 | None |
| MC-NAME-001 | Applicable | PASS | OAuthCorsProperties/OAuthNativeCorsProcessor，原Config/Test精确名 | 角色明确 | None |
| MC-VALID-001 | Applicable | PASS | schema container/AssertTrue、Default Validator、processor tenant/headers/native handlers | 全部新交接受检 | None |
| MC-MODEL-001 | Applicable | PASS | 普通Properties完整Lombok/Builder/default集合与防御复制；无新PO/record | 建模正确，历史UK不改 | None |
| MC-CONVERT-001 | Not applicable | N/A | 七路径不新增对象跨层业务转换，只有技术CORS/header/OAS构建 | 无Converter需求 | None |
| MC-LOG-001 | Applicable | PASS | Processor @Slf4j；配置factory不属业务聚合；无token/password/log payload | 不输出秘密/不新增高频日志 | None |
| MC-BEAN-001 | Applicable | PASS | 具名factory/新qualified delegate；Admin lombok.config复制Qualifier；javap证实参数 | 生成注入正确 | None |
| MC-UTIL-001 | Applicable | PASS | JDK+现Spring/Common/Actuator | 无新工具或Utils | None |
| MC-JSON-001 | Applicable | PASS | OAS/published tests；原POST/GET/Controller wrappers不改 | JSON wire不漂移 | None |
| MC-TIME-001 | Applicable | PASS | 600s CORS header；无Date/Calendar/SimpleDateFormat | 时间约束正确 | None |
| MC-CONFIG-001 | Applicable | PASS | 仅type-bind原key，YAML零diff；HTTP例外local+env+显式allow | 不引入profile漂移，Step10仍未启用 | None |
| MC-PATTERN-001 | Applicable | PASS | 限定Decorator包装DefaultCorsProcessor，原chain内显式filter | Complex复用，约束Simple | None |
| MC-SCOPE-001 | Applicable | PASS | git diff与7 declared paths吻合；docs未混源码scope | 只当前Step | None |
| MC-TEST-001 | Applicable | PASS | RED7:3 failures/0 errors；final exact8/expanded50均0 failure/error/skip | 行为门禁闭合，不是PG/Redis/browser证明 | None |
| MC-BLOCKER-001 | Applicable | PASS | 前16项逐项已闭合；PC005机械hook修正；未决设计None | 允许提交当前Step | None |

- Verified；下一动作仅对七路径精确commit，提交成功并记录HEAD之前不编辑Step2。

### Step 1 Committed

- Commit：`214e53fd1ab819724ef2103e87e0a0c1e8e69c59`；精确七路径，已用git show核对，无其他文件。
- 提交后剩余工作树仅本轮未跟踪Spec/Plan；源码/staged干净。此HEAD已在Step2第一次编辑前记录。
- Committed paths：
  - `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthNativeCorsContractTest.java`
  - `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiContractTest.java`
  - `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthCorsProperties.java`
  - `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthNativeCorsProcessor.java`
  - `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/IdpSecurityConfig.java`
  - `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java`
  - `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/OAuthProtocolOpenApiConfiguration.java`

### Step 2 preflight — Blocked pending dependency-scope approval

基线`214e53fd1ab819724ef2103e87e0a0c1e8e69c59`；Step2未开始任何源码编辑。发现原“零POM/processor改动”前提不成立：

| File to add to approved scope | Current evidence | Minimal proposed change | Owner Step | Impact/verification |
| --- | --- | --- | --- | --- |
| `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-contract/pom.xml` | 当前仅Jackson注解/测试依赖，父POM无compile依赖；没有Jakarta Validation API | 添加`jakarta.validation:jakarta.validation-api`，版本由现Boot3.5.16管理为3.0.2；compile API-only | Step2模型metadata前置 | 纯Contract不加Spring/运行starter；真实Default/cascade schema与JSON回归 |
| `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/pom.xml` | 当前无Lombok compile依赖；授柄/组件Lombok均provided/optional不传递；继承processor path只有Lombok/Boot配置处理器，无MapStruct | 添加`org.projectlombok:lombok` provided/optional、现管理版本1.18.46；按授柄Starter既有配置补MapStruct processor1.6.3及lombok-mapstruct-binding0.2.0；保留继承Lombok/Boot processors，MapStruct API复用既有授柄Starter compile传递 | Step2 converter/组件前置 | 构建期能力，零新runtime库/版本升级；验证真实生成Mapper和Qualifier构造注入 |
| `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/pom.xml` | 无直接Lombok、目前源码无Lombok；上游均provided不传递；Step7新host adapter需Lombok | 添加同管理版本Lombok provided/optional，不改继承处理器 | Step7 adapter前置 | 不扩Admin框架/依赖版本；host adapter/wiring tests |

用户已明确批准（2026-10-10）上述最小POM修订并继续；原Step2依赖BLOCKED已关闭。所有提出的JAR均已在本机既有仓库使用/缓存，不需下载新版本；没有更小的现有纯契约能力替代Jakarta API。若不批准，只能停在此Step，不能删除规范注解、手写Converter/业务注入constructor或跳到Step3。批准后同步Spec/Plan清单、原零POM约束（现仅已批准三项例外）及允许的exact边界门禁；按原Step序执行，Step2只编辑Contract/Starter POM，Admin POM归Step7。

- `MC-DEP-001` / `MC-BLOCKER-001`：批准输入已闭合；执行时按新增精确POM范围和实际编译重新验证；其他已完成Step1保持Committed。

### Step 2 lock — In Progress

基线214e53fd1ab819724ef2103e87e0a0c1e8e69c59已记录；用户批准最小三个POM修订。Step2只含Contract/Starter POM，Admin POM留Step7。MC-DEP/架构/复用基线已闭合，未进行Step2源码编辑，后续按修订顺序先编译前置、行为RED、GREEN、逐门禁再commit。

- PLAN-CLAR-006：拒绝异常复用现有Spring `org.springframework.security.authorization.AuthorizationDeniedException` 的reason constructor，不是不存在的AuthorizationService内嵌类/identity二参ctor。Spec原异常族/403/FIELD_ACCESS_DENIED不变；RuntimeUnavailable仍原内嵌类型含identity。首次test compile错误不计RED，修正测试类型后重新观察行为RED。

### Step 2 Verified gate

- RED已在中断前真实观察：17 tests/5 failures/0 errors/skip，只委托PDP时未拒绝READ/缺字段/不可用/无USER（前置测试异常类型编译错误排除）。
- 中断后精确命令重新完成：/tmp/tq-step2-resumed-green.log/.exit，20 tests/0 failure/error/skip，exit0；10个新guard测试+10个原PDP/方法/字段读回归。
- Generated FieldWriteAuthorizationConverterImpl实际由MapStruct1.6.3生成，只有三个key；真实Spring auto-config具名mapper/guard与MethodValidation正常，Class非final以支持原代理；无HV000151、JSON字段数量不变。普通Command完整Lombok与集合复制，Proof/权限JWT未增加。
- 构建前置限用户批准Contract API/Starter provided及processor；Admin POM未改，留Step7；Validation engine复用已有RPC Starter，不新增。

| Literal rule | Applicability | Status | Diff/path/symbol evidence | Test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | Command/Converter/Component及原request/decision | 语义类型10 paths | 原规范未削弱 | None |
| Rule 2 | Applicable | PASS | WriteFields/default/cascade，输入→生成request→PDP结果都校验，USER先于PDP | 真实Validator/负例/无USER SERVICE/HV000151 tests | 原规范未削弱 | None |
| Rule 3 | Applicable | PASS | Command Data/NoArgs/AllArgs/Accessors/Builder、集合复制；原immutable record保持；BaseForwardConverter | generated Mapper真实三key；构造/集合/JSON tests | 原规范未削弱 | None |
| Rule 4 | Applicable | PASS | Guard Slf4j/RequiredArgsConstructor final Qualified；factory显式名称；parent lombok复制 | Spring proxy/wiring tests与生成构造 | 原规范未削弱 | None |
| Rule 5 | Applicable | PASS | JDK/现Spring/Jakarta/Common，无新工具类 | imports、批准POM范围 | 原规范未削弱 | None |
| Rule 6 | Applicable | PASS | 原decision12字段/FieldAccess2字段保持，无新JSON库 | 真实Jackson测试 | 原规范未削弱 | None |
| Rule 7 | Applicable | PASS | 无profile/key新改动；已有条件不变 | YAML零diff | 原规范未削弱 | None |
| Rule 9 | Applicable | PASS | 固定WRITE集合Simple，原复杂PDP/PEP复用 | actualPDP snapshot/fence/once call tests | 原规范未削弱 | None |
| Rule 10 | Applicable | PASS | 原Instant/Clock，未加Date | Clock.fixture与imports | 原规范未削弱 | None |
| Rule 11 | Applicable | PASS | 只鉴神contract/starter传统技术包、批准两POM与parent lombok | 10 paths范围、无DDL/其他平台 | 原规范未削弱 | None |

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 10路径仅原Contract/Starter/parent配置，不新层 | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-REUSE-001 | Applicable | PASS | PDP/CurrentUser/Common ValidationUtils/BaseForwardConverter/现RPC Validator | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-DEP-001 | Applicable | PASS | 用户批准两POM已落地、managed现版本，无下载/升级；Admin留Step7 | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-NAME-001 | Applicable | PASS | FieldWriteAuthorizationCommand/Converter/Component角色明确 | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-VALID-001 | Applicable | PASS | WriteFields→default request→cascade decision，非法/缺USER先拒绝 | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-MODEL-001 | Applicable | PASS | 普通Command完整Lombok、防御复制；旧decision/request immutable | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-CONVERT-001 | Applicable | PASS | 真实生成Mapper extends BaseForwardConverter，仅三key | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-LOG-001 | Applicable | PASS | 业务guard Slf4j且无secret日志 | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-BEAN-001 | Applicable | PASS | 具名factory/qualified四依赖/parent copyable/真实容器与代理成功 | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-UTIL-001 | Applicable | PASS | 仅JDK/现组件，无自造Utils | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-JSON-001 | Applicable | PASS | 12/2旧JSON字段保持、无新公开API | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-TIME-001 | Applicable | PASS | Clock/Instant不变、无Date类 | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-CONFIG-001 | Applicable | PASS | 无YAML/key变化，已有开启条件保持 | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-PATTERN-001 | Applicable | PASS | Simple集合WRITE检查复用原复杂PDP，不新规则引擎 | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-SCOPE-001 | Applicable | PASS | 只10 declared路径；两个POM获人类明确批准 | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-TEST-001 | Applicable | PASS | fresh20 tests/0fail/error/skip、RED已记录、含真实PDP与Spring代理 | 当前Step源/受控门禁满足，runtime不宣称 | None |
| MC-BLOCKER-001 | Applicable | PASS | 全部16项已闭合，无未批准依赖/范围 | 当前Step源/受控门禁满足，runtime不宣称 | None |

- Verified；仅提交这10路径，不包含未跟踪Spec/Plan。

### Step 2 Committed

- Commit `26f77b7c26b8253973c4761d507aa5faada43a47`，已核对精确10路径。源码/staged干净，仅本轮Spec/Plan未跟踪；HEAD与路径在Step3首编前记录。
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/pom.xml`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-contract/pom.xml`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/authorization/AuthorizationService.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-contract/src/main/java/top/egon/cola/platform/tianquan/jianshen/contract/authorization/FieldPolicyDecision.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/field/FieldWriteAuthorizationCommand.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/field/FieldWriteAuthorizationConverter.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/field/Rbac3FieldWriteAuthorizationComponent.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/test/java/top/egon/cola/platform/tianquan/jianshen/starter/field/Rbac3FieldWriteAuthorizationComponentTest.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/lombok.config`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/autoconfigure/Rbac3StarterAutoConfiguration.java`

### Step 3 Verified gate

- 基线26f77b7c26b8253973c4761d507aa5faada43a47；两路径独占，依赖None（实际串行前置已提交）。
- RED：/tmp/tq-step3-red.log/exit，8 tests/1 failure/0 errors/skip，spool满时旧委托仍返回broker成功。GREEN：/tmp/tq-step3-final-green.log/exit，10 tests/0failure/error/skip，exit0。
- strict保留frame schema1与原3参Lombok构造；新增Valid/Validated使用现宿主方法校验，record自身不变量保持。class非final允许既有代理；旧append/replay/ack语义在此Step不改。文件force/目录force/不支持atomic/满、MQ未知/重投均有负作用断言，不代表真实卷掉电验收。

| Literal rule | Applicability | Status | Diff/path/symbol evidence | Test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | 原Component/Event/Test角色，不增加carrier | fresh10 tests及diff审阅 | 规范保持 | None |
| Rule 2 | Applicable | PASS | strict Valid+Validated；原immutable Event ctor/schema确保typed输入；默认宿主method validation | fresh10 tests及diff审阅 | 规范保持 | None |
| Rule 3 | Applicable | PASS | 无新POJO/Converter，旧immutable Event和RequiredArgs构造保留 | fresh10 tests及diff审阅 | 规范保持 | None |
| Rule 4 | Applicable | PASS | 原Slf4j/RequiredArgs/qualified三字段与原named factory；无手写注入 | fresh10 tests及diff审阅 | 规范保持 | None |
| Rule 5 | Applicable | PASS | 仅JDK FileChannel/Files与原Redisson/Jackson，不造工具框架 | fresh10 tests及diff审阅 | 规范保持 | None |
| Rule 6 | Applicable | PASS | 原schema1/11字段/CRC/MAC序列化不变 | fresh10 tests及diff审阅 | 规范保持 | None |
| Rule 7 | Applicable | PASS | 未改Properties/YAML/开关，strict还未被其他host开启 | fresh10 tests及diff审阅 | 规范保持 | None |
| Rule 9 | Applicable | PASS | 扩原STAGED→SENT技术State，不新增状态框架/业务分支 | fresh10 tests及diff审阅 | 规范保持 | None |
| Rule 10 | Applicable | PASS | 无新日期实体；持久化JDK filesystem API，Date禁用 | fresh10 tests及diff审阅 | 规范保持 | None |
| Rule 11 | Applicable | PASS | 两SS audit路径原传统SDK，零DB/POM/其他平台修改 | fresh10 tests及diff审阅 | 规范保持 | None |

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原SS audit职责和两路径 | 当前Step源/受控门禁闭合 | None |
| MC-REUSE-001 | Applicable | PASS | 原frame/Files/FileChannel/Streams/Validation | 当前Step源/受控门禁闭合 | None |
| MC-DEP-001 | Applicable | PASS | 零POM/库新增 | 当前Step源/受控门禁闭合 | None |
| MC-NAME-001 | Applicable | PASS | 原组件/事件角色保持 | 当前Step源/受控门禁闭合 | None |
| MC-VALID-001 | Applicable | PASS | Valid/Validated与Event immutable构造验证 | 当前Step源/受控门禁闭合 | None |
| MC-MODEL-001 | Applicable | PASS | 无新实体，原record语义保持 | 当前Step源/受控门禁闭合 | None |
| MC-CONVERT-001 | Not applicable | N/A | 没有新增业务模型投影/Converter | 当前Step源/受控门禁闭合 | None |
| MC-LOG-001 | Applicable | PASS | 原Slf4j/安全eventId日志，无raw token | 当前Step源/受控门禁闭合 | None |
| MC-BEAN-001 | Applicable | PASS | 原三依赖final Qualifier/RequiredArgs及原factory名不改 | 当前Step源/受控门禁闭合 | None |
| MC-UTIL-001 | Applicable | PASS | JDK与现批准库 | 当前Step源/受控门禁闭合 | None |
| MC-JSON-001 | Applicable | PASS | 原11字段/schema/CRC/MAC保持 | 当前Step源/受控门禁闭合 | None |
| MC-TIME-001 | Applicable | PASS | 未增加日期类型，FS接口原生持久化 | 当前Step源/受控门禁闭合 | None |
| MC-CONFIG-001 | Applicable | PASS | 零key/profile变化，其他host旧append不被开启 | 当前Step源/受控门禁闭合 | None |
| MC-PATTERN-001 | Applicable | PASS | 原技术State扩strict路径，兼容delegate保留 | 当前Step源/受控门禁闭合 | None |
| MC-SCOPE-001 | Applicable | PASS | 两声明路径diff、无SQL/POM/其他模块 | 当前Step源/受控门禁闭合 | None |
| MC-TEST-001 | Applicable | PASS | fresh10测试/0fail/error/skip，spool/MQ/fault覆盖 | 当前Step源/受控门禁闭合 | None |
| MC-BLOCKER-001 | Applicable | PASS | 逐项已闭合，真实卷/Redis验收仍用户控制 | 当前Step源/受控门禁闭合 | None |

### Step 3 Committed

- `cb75396968385944a4c4341ab92cae3b26d908f5`；两路径已核对，源码/staged干净，仅本轮Spec/Plan未跟踪。HEAD在Step4第一编辑前记录。
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponentTest.java`

### Step 4 lock — In Progress

基线cb75396968385944a4c4341ab92cae3b26d908f5已记录；前置Step3提交完成。范围仅章7 Step4八个独立路径，先建立Delivery/Receipt编译契约，再行为RED与consumer/producer闭环。原save已是事务内sameEvent幂等对账，不需改SQL/Repository。当前不得进入Step5或提交编译前置为完成。

- Step4当前证据：/tmp/tq-step4-red.log exit1（缺receipt仍盲删sender帧）；增量独立目录测试/tmp/tq-step4-incremental2.log exit0，1 test。仅说明缺确认时保留帧，不表示完整Step4 GREEN。consumer receipt→XACK、伪造/TTL/DEAD/有界cursor等仍必须完成并验证，禁止当前提交为完成或进入Step5。

### Step 4 verification — Verified

- Baseline: `cb75396968385944a4c4341ab92cae3b26d908f5`；REQ-001/002/006/007/008/014/015/018。
- RED沿用已记录`/tmp/tq-step4-red.log`：receipt缺失仍删除发送方事实（1 behavior failure，非编译/环境）。
- Fresh GREEN：§8 Step4 exact Maven command，`/tmp/tq-step4-commit-green.log`、exit文件为0；Starter10 + receipt10 + consumer15 = **35 tests，0 failure/error/skip**。直接使用真实codec/temp卷/Clock；consumer DB/Redis为受控double，未运行外部系统。
- Source review：原MP `OAuthAuditRepositoryImpl#save` @Transactional→sameEvent/版本/tenant校验→返回；consumer返回后原子Lua fingerprint guard + SET PX再XACK。Lua实际PRIMARY执行、commit unknown和掉电验收仍Runtime unverified。
- Step4 mechanical details：Clock/Validator为批准factory必要参数（组件generated6参），旧3参兼容适配提供默认值；consumer旧6参适配保留并以generated7参Autowired选择注入；onConstructor不属于field injection。孤立SENT按batch游标多轮回收，不破坏有界合同。
- 文档mechanical repair：§8批准3 POM的git exclude pathspec改为可执行命令；§5 POM表行恢复连续。未改变既有批准scope/行为。

| Literal rule | Applicability | Status | Diff/path/symbol evidence | Test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | Properties/ReceiptDTO/Component/Consumer实际角色，未新增模糊carrier | 8 declared paths编译通过 | 原规范保持 | None |
| Rule 2 | Applicable | PASS | Event构造与@Valid；具名defaultValidator/Common实例校验receipt、replay、codec；原MP save校验保持 | schema负例/strict Jackson/真实容器注入/35测试 | 原规范保持 | None |
| Rule 3 | Applicable | PASS | 普通Delivery Properties和ReceiptDTO完整Lombok Builder；原不可变Event不变；receipt仅技术codec | DTO生成编译/6字段及独立golden fingerprint/MAC | 原规范保持 | None |
| Rule 4 | Applicable | PASS | 原Slf4j/具名Beans；新增final Qualified依赖；Lombok generated constructor及consumer onConstructor Autowired；旧构造技术适配保留 | 参数Qualifier与真实Spring consumer装配tests | 原规范保持 | None |
| Rule 5 | Applicable | PASS | JDK Files/FileChannel/Mac/SHA256/现Redisson/Common，未引入Utils或依赖 | POM/其他平台diff为零，离线build | 原规范保持 | None |
| Rule 6 | Applicable | PASS | Boot ObjectMapper；receipt固定6字段/ISO Instant/unknown及重复字段拒绝；原Event11 frame不变 | golden/伪schema-MAC-id-fingerprint/浮点及字符串version负例 | 原规范保持 | None |
| Rule 7 | Applicable | PASS | 本Step只typed delivery默认值与绑定；YAML未改，六profile默认结构相同；Step10尚未启用 | git diff profile零变更/Duration-batch约束测试 | 原规范保持 | None |
| Rule 9 | Applicable | PASS | 复用原技术State：STAGED evt→SENT原子marker→verified STORED/EXPIRED cleanup或DEAD marker；cursor恢复 | DB→receipt→XACK顺序、失败PEL/DEAD/restart/batch测试 | 原规范保持 | None |
| Rule 10 | Applicable | PASS | Clock/Instant/Duration/FileTime；实际marker mtime持久化并按timeout判定；无Date | 固定Clock超时/restart，禁止日期imports无匹配 | 原规范保持 | None |
| Rule 11 | Applicable | PASS | 只原SS audit/autoconfigure及SA consumer/test；复用原MP事务/Streams；零DDL/新层 | 8 paths、SQL/其他平台字节gate、35受控测试 | 原规范保持 | None |

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原SDK技术包与Admin service.impl保留；零新架构层 | 当前Step边界闭合；runtime不宣称 | None |
| MC-REUSE-001 | Applicable | PASS | 现Common ValidationUtils/Boot Jackson/Files/Redisson/原MP save事务及sameEvent复用 | 当前Step边界闭合；runtime不宣称 | None |
| MC-DEP-001 | Applicable | PASS | 本Step所有POM零diff，offline不下载；零新依赖 | 当前Step边界闭合；runtime不宣称 | None |
| MC-NAME-001 | Applicable | PASS | AuditDeliveryProperties、TokenUsageAuditReceiptDTO及原行为类型准确 | 当前Step边界闭合；runtime不宣称 | None |
| MC-VALID-001 | Applicable | PASS | 具名Validator、Valid入口、codec/replay手动Default、parse/HMAC/原save形状校验；负例已执行 | 当前Step边界闭合；runtime不宣称 | None |
| MC-MODEL-001 | Applicable | PASS | 两新普通class完整Lombok；不可变Event原schema保持；CRC/HMAC原frame兼容 | 当前Step边界闭合；runtime不宣称 | None |
| MC-CONVERT-001 | Not applicable | N/A | N/A：只有技术frame/receipt codec；无业务层新模型projection/Converter，Step6另有批准Mapper | 当前Step边界闭合；runtime不宣称 | None |
| MC-LOG-001 | Applicable | PASS | 原Slf4j仅safe eventId/failureType；无token/key/secret/payload输出 | 当前Step边界闭合；runtime不宣称 | None |
| MC-BEAN-001 | Applicable | PASS | named AuditDeliveryProps与组件factory；generated6/7参数Qualifier验证，legacy3/6调用保持，真实容器通过 | 当前Step边界闭合；runtime不宣称 | None |
| MC-UTIL-001 | Applicable | PASS | 仅JDK与现有Spring/Common/Redisson；未创建Utils或替代JSON | 当前Step边界闭合；runtime不宣称 | None |
| MC-JSON-001 | Applicable | PASS | strict 6字段shape/duplicate/unknown/annotation校验；ISO confirmedAt、golden编码；旧Event11不变 | 当前Step边界闭合；runtime不宣称 | None |
| MC-TIME-001 | Applicable | PASS | Clock+Duration边界、SENT mtime重写metadata force；restart/timeout真实文件tests | 当前Step边界闭合；runtime不宣称 | None |
| MC-CONFIG-001 | Applicable | PASS | 新的typed Delivery defaults三key有界，YAML零diff/未启usage；绑定复用Boot properties | 当前Step边界闭合；runtime不宣称 | None |
| MC-PATTERN-001 | Applicable | PASS | 原filesystem-backed技术State participants保持；确认与重放恢复有真实transition证据，未引入另一引擎 | 当前Step边界闭合；runtime不宣称 | None |
| MC-SCOPE-001 | Applicable | PASS | 与Step4八文件逐项吻合；source阶段docs未混入；SQL/POM/模板/其他平台零diff | 当前Step边界闭合；runtime不宣称 | None |
| MC-TEST-001 | Applicable | PASS | RED缺receipt盲删已记录；final exact离线命令35 tests/0 failures/errors/skips；diff及Plan strict PASS | 当前Step边界闭合；runtime不宣称 | None |
| MC-BLOCKER-001 | Applicable | PASS | 前16项逐项闭合；DB PRIMARY/Redis Lua/failover/真实卷掉电仍Runtime unverified，不冒充runtime通过 | 当前Step边界闭合；runtime不宣称 | None |

### Step 4 committed

- Commit: `dcf282aa72d27d89e19edcc09325c2184328df53`；message `fix(shoubing): confirm audit delivery across nodes`。
- Fresh GREEN35与门禁如上，commit路径验证恰好8项：
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumer.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/TokenUsageAuditReceiptContractTest.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/audit/service/impl/OAuthAuditConsumerTest.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/AuditDeliveryProperties.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/domain/dto/TokenUsageAuditReceiptDTO.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponentTest.java`

- 已验证新HEAD与source范围；仅Spec/Plan任务文档untracked，未改其他工作。Step5首次编辑前已记录本hash和paths。

### Step 5 lock — In Progress

- Baseline `dcf282aa72d27d89e19edcc09325c2184328df53`；Step4已Committed且paths已记录；严格只Step5八个unique路径。按File1/2→兼容签名→真实RED→verify/cache/acquire观察→门禁/commit，尚未启host。
- Requirements REQ-001/002/009/010/011/014/018；§8 exact Starter focused command；旧2/3参manager、旧SERVICE factories、typed final wrappers、cache key/single-flight保持。
- 复核：实际JWK TTL使用System.nanoTime（并非Plan简写Clock），保留原monotonic语义；验签必须包Nimbus verifier.verify，URI/claim/Redis均在其外。
- Literal lock：

| Literal rule | Applicability | Status | Evidence |
| --- | --- | --- | --- |
| Rule 1 | Applicable | Pending final diff | Step5载体/port、原required-args/qualifier、固定Decorator/Observer与实际验证/缓存，前置证据已复核 |
| Rule 2 | Applicable | Pending final diff | Step5载体/port、原required-args/qualifier、固定Decorator/Observer与实际验证/缓存，前置证据已复核 |
| Rule 3 | Applicable | Pending final diff | Step5载体/port、原required-args/qualifier、固定Decorator/Observer与实际验证/缓存，前置证据已复核 |
| Rule 4 | Applicable | Pending final diff | Step5载体/port、原required-args/qualifier、固定Decorator/Observer与实际验证/缓存，前置证据已复核 |
| Rule 5 | Applicable | Pending final diff | Step5载体/port、原required-args/qualifier、固定Decorator/Observer与实际验证/缓存，前置证据已复核 |
| Rule 6 | Applicable | Pending final diff | Step5载体/port、原required-args/qualifier、固定Decorator/Observer与实际验证/缓存，前置证据已复核 |
| Rule 7 | Applicable | Pending final diff | Step5载体/port、原required-args/qualifier、固定Decorator/Observer与实际验证/缓存，前置证据已复核 |
| Rule 9 | Applicable | Pending final diff | Step5载体/port、原required-args/qualifier、固定Decorator/Observer与实际验证/缓存，前置证据已复核 |
| Rule 10 | Applicable | Pending final diff | Step5载体/port、原required-args/qualifier、固定Decorator/Observer与实际验证/缓存，前置证据已复核 |
| Rule 11 | Applicable | Pending final diff | Step5载体/port、原required-args/qualifier、固定Decorator/Observer与实际验证/缓存，前置证据已复核 |

| Check ID | Applicability | Status | Evidence |
| --- | --- | --- | --- |
| MC-ARCH-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-REUSE-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-DEP-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-NAME-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-VALID-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-MODEL-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-CONVERT-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-LOG-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-BEAN-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-UTIL-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-JSON-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-TIME-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-CONFIG-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-PATTERN-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-SCOPE-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-TEST-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |
| MC-BLOCKER-001 | Applicable pending diff review | Pending final gate | Step5指定路径/Core validation/Common与Nimbus hook/原缓存复用；offline、零POM/DB/其他平台变更 |

### Step 5 verification — Verified

- RED：`/tmp/tq-step5-behavior-red.log` exit1，26 tests/2 failures/0 errors，分别真实crypto与SERVICE cache执行后零样本。之前AssertJ API编译失败已修复，不计RED。
- Fresh exact GREEN：§8 Step5 command，`/tmp/tq-step5-commit-green.log` exit0，MultiIssuer14+Client16=**30 tests/0 failures/errors/skips**。扩展`IdpStarterAutoConfigurationTest`在`/tmp/tq-step5-green3.log`37 tests/0 failure/error/skip（当时MultiIssuer13）；新增constructor/单飞观察随后exact30再次验证。
- Mechanical details：cache继续原nano TTL，不用Clock替换其语义；recorder固定startup装进manager/cache/client，Verifier callback仅交完整Valid对应VerifiedJwtBO，同一原验证一次，无每请求重新绑定recorder/ThreadLocal。Core纯端口没有Jackson依赖；JDK JavaBeans Transient由真实Boot Jackson测试证明隐藏assert getter。
- 验证范围：MockRestServiceServer+实际RSA/原Nimbus decoder/真实缓存、受控revocation/client-manager；未运行Redis/PG/部署/负载，不声明p95或99%目标达成。既有Netty macOS DNS native fallback日志不影响命名tests，不添加平台依赖。

| Literal rule | Applicability | Status | Diff/path/symbol evidence | Test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | SecurityMetricObservationBO/Core port、ObservedJwsVerifier/BoundedJwkSetCache等角色明确 | fresh exact30/expanded37及diff逐行审阅 | 原规则保持 | None |
| Rule 2 | Applicable | PASS | BO原生约束+跨字段AssertTrue，record接口Valid；VerifiedJwtBO原validate与完整revocation后callback；负例schema测试 | fresh exact30/expanded37及diff逐行审阅 | 原规则保持 | None |
| Rule 3 | Applicable | PASS | 普通BO完整Data/NoArgs/AllArgs/Builder/Accessors；Immutable Verified/CacheEntry保持；无新模型层转换 | fresh exact30/expanded37及diff逐行审阅 | 原规则保持 | None |
| Rule 4 | Applicable | PASS | 现Slf4j/RequiredArgs/final qualified recorder；generated4参Qualifier证据、named factory/provider，异常logger共享一次flag | fresh exact30/expanded37及diff逐行审阅 | 原规则保持 | None |
| Rule 5 | Applicable | PASS | 仅JDK计时/AtomicBoolean与现Spring/Nimbus/Common；Core不引入Jackson/Micrometer/新POM | fresh exact30/expanded37及diff逐行审阅 | 原规则保持 | None |
| Rule 6 | Applicable | PASS | BO四字段Boot Jackson实测；JDK JavaBeans Transient排除校验属性，原typed verification和JWT schema保持 | fresh exact30/expanded37及diff逐行审阅 | 原规则保持 | None |
| Rule 7 | Applicable | PASS | 零YAML/key修改；fixed Core port在startup绑定；自身usage仍未启用 | fresh exact30/expanded37及diff逐行审阅 | 原规则保持 | None |
| Rule 9 | Applicable | PASS | Nimbus JWSVerifierFactory/JWSVerifier真实Decorator，cache/client→Core Observer；不制造另一验证链 | fresh exact30/expanded37及diff逐行审阅 | 原规则保持 | None |
| Rule 10 | Applicable | PASS | 实际verify及实际acquire用System.nanoTime→Duration；JWK原monotonic TTL不改，SERVICE仍Clock/原renewalSkew | fresh exact30/expanded37及diff逐行审阅 | 原规则保持 | None |
| Rule 11 | Applicable | PASS | 仅SC observability和既有SS包，原Core纯端口→Starter→host方向；无第三层/DB/SQL | fresh exact30/expanded37及diff逐行审阅 | 原规则保持 | None |

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原Core/Starter依赖方向与八声明路径，无host反向引用 | 当前Step门禁闭合，runtime未验证 | None |
| MC-REUSE-001 | Applicable | PASS | 实际Nimbus processor/JWSVerifierFactory、Spring Cache与原SERVICE key/single-flight复用 | 当前Step门禁闭合，runtime未验证 | None |
| MC-DEP-001 | Applicable | PASS | 零POM变更；Core无Jackson API故采用JDK JavaBeans Transient且四字段JSON实测，无引入 | 当前Step门禁闭合，runtime未验证 | None |
| MC-NAME-001 | Applicable | PASS | 新BO/Recorder/Verifier精确角色，原CacheEntry不变 | 当前Step门禁闭合，runtime未验证 | None |
| MC-VALID-001 | Applicable | PASS | NotBlank/Pattern/PositiveOrZero/AssertTrue、接口Valid及原Verified.validate；恶意signal/result/value/duration真实Validator负例 | 当前Step门禁闭合，runtime未验证 | None |
| MC-MODEL-001 | Applicable | PASS | 普通BO完整Lombok baseline+generated compile；原immutable record语义保持 | 当前Step门禁闭合，runtime未验证 | None |
| MC-CONVERT-001 | Not applicable | N/A | N/A：技术固定sample构造、已验证BO capture同一对象，无业务投影新Converter | 当前Step门禁闭合，runtime未验证 | None |
| MC-LOG-001 | Applicable | PASS | 所有业务Slf4j；一次共享flag限制metric异常日志；仅failureType无身份/token/key | 当前Step门禁闭合，runtime未验证 | None |
| MC-BEAN-001 | Applicable | PASS | 旧manager2/3参与SERVICE factory保持；新增generated4参Qualifier实测；Core provider具名，auto config8 regression通过 | 当前Step门禁闭合，runtime未验证 | None |
| MC-UTIL-001 | Applicable | PASS | 只JDK/Nimbus/Spring/现组件；无Utils或新依赖 | 当前Step门禁闭合，runtime未验证 | None |
| MC-JSON-001 | Applicable | PASS | Boot Jackson BO仅4字段，JDK Transient排除schema方法；JWT/Valid wire未扩张 | 当前Step门禁闭合，runtime未验证 | None |
| MC-TIME-001 | Applicable | PASS | 实际验签finally Timer、cache nanos expiry、acquire真实单飞winner；固定Clock/真实过期分支与延迟Redis测试 | 当前Step门禁闭合，runtime未验证 | None |
| MC-CONFIG-001 | Applicable | PASS | 没有profile差异；新host port尚未安装/启动；Provider空→NOOP兼容 | 当前Step门禁闭合，runtime未验证 | None |
| MC-PATTERN-001 | Applicable | PASS | 实际Decorator委托provider/algorithms/verify；Observer通过Core port，保留原验证顺序 | 当前Step门禁闭合，runtime未验证 | None |
| MC-SCOPE-001 | Applicable | PASS | 八unique paths、仅当前Step；POM/SQL/模板/其他平台/shared零diff | 当前Step门禁闭合，runtime未验证 | None |
| MC-TEST-001 | Applicable | PASS | 行为RED26/2 failure/0 error；fresh exact GREEN30/0失败错误skip；expanded37含auto config8；单飞实际acquire Timer一次 | 当前Step门禁闭合，runtime未验证 | None |
| MC-BLOCKER-001 | Applicable | PASS | 前16行闭合，无未批准文件/依赖；真实负载/p95/缓存命中率及外部服务仍Runtime unverified | 当前Step门禁闭合，runtime未验证 | None |

### Step 5 committed

- Commit: `8a4a30c8266cc3d520a770322972bd2147d493cb`；message `feat(shoubing): observe real token verification and caches`。
- 已核对新HEAD、source只有8路径；fresh exact30/expanded37、逐项Literal/MC如上。
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/observability/IdpSecurityMetricsRecorder.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-core/src/main/java/top/egon/cola/platform/tianquan/shoubing/core/observability/SecurityMetricObservationBO.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2Client.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/TenantIssuerAuthenticationManagerComponent.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/client/IdpServiceOAuth2ClientTest.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/MultiIssuerResourceServerTest.java`

- Step6首次编辑前已记录本hash与paths；Spec/Plan仍untracked任务文档，其他工作保留。

### Step 6 preflight clarification and lock — In Progress

- Baseline `8a4a30c8266cc3d520a770322972bd2147d493cb`；前置Step3/4/5全部Committed，首次源码编辑前hash/path记录已闭合。
- Scope clarification PC-008：原Plan incorrectly assumed producer.reference exists；Spec §8.3/10.5/11.1已经规定producer-owned HMAC helper，文件本来位于66-path库存。依据用户最新AGENTS “If an additional file must be changed ... another direct dependency ... allowed only when ... necessary for correctness”，将该现有文件补进Step6（14 unique paths），不新增模块/依赖/行为范围，不把key读取复制到Mapper。
- Step6 required tests/rollback/requirements沿§7/8；本Step新types/contract→signature scaffold→RED→strict handoff→完整门禁→commit。

| Literal rule | Applicability | Status | Evidence |
| --- | --- | --- | --- |
| Rule 1 | Applicable | Pending final gate | Step6 declared payload/proof/Mapper/observer/filter/RPC/factory、原安全链/secret边界；PC008仅必要依赖，前置状态保持 |
| Rule 2 | Applicable | Pending final gate | Step6 declared payload/proof/Mapper/observer/filter/RPC/factory、原安全链/secret边界；PC008仅必要依赖，前置状态保持 |
| Rule 3 | Applicable | Pending final gate | Step6 declared payload/proof/Mapper/observer/filter/RPC/factory、原安全链/secret边界；PC008仅必要依赖，前置状态保持 |
| Rule 4 | Applicable | Pending final gate | Step6 declared payload/proof/Mapper/observer/filter/RPC/factory、原安全链/secret边界；PC008仅必要依赖，前置状态保持 |
| Rule 5 | Applicable | Pending final gate | Step6 declared payload/proof/Mapper/observer/filter/RPC/factory、原安全链/secret边界；PC008仅必要依赖，前置状态保持 |
| Rule 6 | Applicable | Pending final gate | Step6 declared payload/proof/Mapper/observer/filter/RPC/factory、原安全链/secret边界；PC008仅必要依赖，前置状态保持 |
| Rule 7 | Applicable | Pending final gate | Step6 declared payload/proof/Mapper/observer/filter/RPC/factory、原安全链/secret边界；PC008仅必要依赖，前置状态保持 |
| Rule 9 | Applicable | Pending final gate | Step6 declared payload/proof/Mapper/observer/filter/RPC/factory、原安全链/secret边界；PC008仅必要依赖，前置状态保持 |
| Rule 10 | Applicable | Pending final gate | Step6 declared payload/proof/Mapper/observer/filter/RPC/factory、原安全链/secret边界；PC008仅必要依赖，前置状态保持 |
| Rule 11 | Applicable | Pending final gate | Step6 declared payload/proof/Mapper/observer/filter/RPC/factory、原安全链/secret边界；PC008仅必要依赖，前置状态保持 |

| Check ID | Applicability | Status | Evidence |
| --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-REUSE-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-DEP-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-NAME-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-VALID-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-MODEL-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-CONVERT-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-LOG-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-BEAN-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-UTIL-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-JSON-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-TIME-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-CONFIG-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-PATTERN-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-SCOPE-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-TEST-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |
| MC-BLOCKER-001 | Applicable | Pending final gate | 14 unique自身Starter声明paths；复用原Common/MapStruct/MQ/Context；无新POM/SQL/线程上下文 |

### PC-009 scope bookkeeping

- 复核后仅`IdpStarterAutoConfigurationTest`需要修正，MultiIssuer已具名不改。此test原来仅作为选择器被引用、未列为写入文件；现依最新用户必要依赖修复授权补齐Spec/Plan库存，66 unique source paths；Step6 source paths15。早期Step记录中的65-path是当时版本的历史证据，不表示修改过历史提交。

### Step 6 verification — Verified

- Baseline: `8a4a30c82`；REQ-001/002/004/005/006/009/014/015/016/018。
- RED `/tmp/tq-step6-red.log` exit1：30 tests/3 expected behavior failures/0 errors（handoff-before-business、匿名失败Event、producer.reference）。
- Fresh exact GREEN：§8 Step6 command，`/tmp/tq-step6-green7.log` exit0：MultiIssuer14 + RPC5 + Observation14 + AutoConfiguration8 = **41 tests/0 failures/errors/skips**。原SERVICE fixture所缺旧source_biz/source_app/source_env/credential_id/app_id已校正，空credentials保持原空字符串合同，不改生产验证来迎合fixture。
- Generated source review：实际Mapper构造原11-field Event；trustedIssuer/资源/subject-client SHA256/token HMAC来自真实验证BO，失败仅fallback。Valid/cast/copy保留proof而公开JSON仍principal/scopes。
- Scope revisions PC008/009如上：latest AGENTS授权必要依赖修改；helper原Spec已规定，旧test只命名3处Bean（4个测试依赖这些fixture）并保持全部原断言。Scope15/unique inventory66，文档未混source commit。未启动服务/PG/Redis/browser，不声明SELF-RUNTIME。

| Literal rule | Applicability | Status | Diff/path/symbol evidence | Test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | ProofBO/ContextBO/Properties/Converter/Component/Filter/原Context角色准确 | fresh exact41/generated/逐行diff review | 原规则保持 | None |
| Rule 2 | Applicable | PASS | Default/cascade proof/context/Event/captured BO；原Valid集合不变量；producer reference原生注解+native guards；开关schema/PostConstruct能力检查 | fresh exact41/generated/逐行diff review | 原规则保持 | None |
| Rule 3 | Applicable | PASS | Proof immutable record；Context/Properties完整Lombok；新MapStruct extends BaseForwardConverter，generated投影两个真实入口 | fresh exact41/generated/逐行diff review | 原规则保持 | None |
| Rule 4 | Applicable | PASS | 业务Slf4j/final qualified Lombok依赖/named factories；URI为factory传入配置值，非新Bean；原compat constructors/factories保持 | fresh exact41/generated/逐行diff review | 原规则保持 | None |
| Rule 5 | Applicable | PASS | JDK Mac/SHA256/UUID/AtomicReference/Clock与现组件；无Utils/新依赖/反射生产mapping | fresh exact41/generated/逐行diff review | 原规则保持 | None |
| Rule 6 | Applicable | PASS | Valid JSON仍2字段；proof JsonIgnore；Context verifiedJwt JsonIgnore/ToString.Exclude；Event原11字段和signature不变 | fresh exact41/generated/逐行diff review | 原规则保持 | None |
| Rule 7 | Applicable | PASS | 新typed props默认disabled；YAML零改动；禁缺依赖静默skip；自身启用留Step10 | fresh exact41/generated/逐行diff review | 原规则保持 | None |
| Rule 9 | Applicable | PASS | 原验证的外层Decorator→真实Mapper→producer State；当前request pre-render callback bridge，不新decoder/ThreadLocal | fresh exact41/generated/逐行diff review | 原规则保持 | None |
| Rule 10 | Applicable | PASS | Clock/Instant/受限trace；server eventId；原crypto Timer不扩到审计/Redis | fresh exact41/generated/逐行diff review | 原规则保持 | None |
| Rule 11 | Applicable | PASS | 15 paths只本Starter及必要existing fixture；Common/MapStruct/Streams复用，无新模块/DDL/外平台 | fresh exact41/generated/逐行diff review | 原规则保持 | None |

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原SS security/audit/rpc/auto config结构保持，无新域层或host反依赖 | 当前Step源/受控证据闭合 | None |
| MC-REUSE-001 | Applicable | PASS | 原一次Nimbus验证、Common ValidationUtils、MapStruct/BaseForward、producer key/mac/strict与gRPC Context复用 | 当前Step源/受控证据闭合 | None |
| MC-DEP-001 | Applicable | PASS | 零POM/依赖/SQL变化；JDK与既有工具链离线验证 | 当前Step源/受控证据闭合 | None |
| MC-NAME-001 | Applicable | PASS | 新BO/Properties/Converter/Component/Filter均语义角色；无模糊carrier | 当前Step源/受控证据闭合 | None |
| MC-VALID-001 | Applicable | PASS | native reference输入/不可变proof ctor、Default/cascade context/Event/Verified BO与Props；负例与启动缺依赖真实断言 | 当前Step源/受控证据闭合 | None |
| MC-MODEL-001 | Applicable | PASS | Proof URI/String不可变record；普通Context/Props完整Lombok；原Valid scope defensive copy保持 | 当前Step源/受控证据闭合 | None |
| MC-CONVERT-001 | Applicable | PASS | 真正generated TokenUsageAuditEventConverterImpl implements BaseForwardConverter；11 Event字段与toProof HASH/HMAC明示映射，无手copy业务模型 | 当前Step源/受控证据闭合 | None |
| MC-LOG-001 | Applicable | PASS | 业务Slf4j；secret-free failureType、一次故障flag；Context raw验证BO排除toString；实际payload/log合同tests | 当前Step源/受控证据闭合 | None |
| MC-BEAN-001 | Applicable | PASS | 具名factory/qualified新增依赖、旧typed wrappers不变；observer generated7参（resource为配置值）与producer既有6参具名依赖；enabled真实容器/disabled Servlet registration，fixtures按Boot命名 | 当前Step源/受控证据闭合 | None |
| MC-UTIL-001 | Applicable | PASS | 仅JDK/Spring/Jackson/Common/现Nimbus；无新Utils/栈 | 当前Step源/受控证据闭合 | None |
| MC-JSON-001 | Applicable | PASS | Valid2字段/proof隐藏、Context raw字段隐藏、原schema1 Event/frame；真实序列化断言 | 当前Step源/受控证据闭合 | None |
| MC-TIME-001 | Applicable | PASS | Clock/Instant不变，server trace/MDC finally恢复；未引入Date（existing fixture imports未新增） | 当前Step源/受控证据闭合 | None |
| MC-CONFIG-001 | Applicable | PASS | usage defaults false typed URI；enabled缺producer/metrics/Validator失败；filter只显式chain启用，无profile变更 | 当前Step源/受控证据闭合 | None |
| MC-PATTERN-001 | Applicable | PASS | 验证外层Decorator只一次；HTTP request阶段callbacks与原gRPC Context，无第二验证链/自建ThreadLocal | 当前Step源/受控证据闭合 | None |
| MC-SCOPE-001 | Applicable | PASS | PC008原producer helper/PC009仅一份fixture必要修正依据最新用户AGENTS；库存66/Step6 fifteen paths；其他平台/模板/SQL不变 | 当前Step源/受控证据闭合 | None |
| MC-TEST-001 | Applicable | PASS | RED30/3 behavior failures/0error；fresh exact GREEN41/0fail/error/skip；实际RSA/User+Service/Mapper/spool、五RPC回调取消、503零业务、过期/匿名/去重 | 当前Step源/受控证据闭合 | None |
| MC-BLOCKER-001 | Applicable | PASS | 前16项已闭合；producer原型及oldJSON/Scope保持，controlled不冒充PG/Redis/多进程/runtime proof | 当前Step源/受控证据闭合 | None |

### Step 6 committed

- Commit `a415c8aef957e64e266b053b7ca2bfa384330b3f`；`fix(shoubing): audit protected HTTP and RPC token use`，已核对15 committed paths及exact GREEN41。
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/OAuthUsageObservationProperties.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/AccessTokenVerification.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpBearerAuthenticationFilter.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpJwtVerifier.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpObservedVerificationComponent.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpSecurityObservationFilter.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/TokenAuditContextBO.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/TokenAuditProofBO.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/converter/TokenUsageAuditEventConverter.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/rpc/IdpRpcBearerServerInterceptor.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/rpc/IdpRpcSecurityContext.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfigurationTest.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/test/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpSecurityObservationFilterTest.java`

- Step7首次编辑前已记录新HEAD/paths；source只本Step，Spec/Plan继续untracked任务文档，其他工作保留。PC008/009必要依赖修正/最新用户授权证据见上；SELF-RUNTIME不执行。

### Step 7 lock — In Progress

- Baseline `a415c8aef957e64e266b053b7ca2bfa384330b3f`，Steps5/6已Committed；9 declared unique paths；Host类/契约→RED→实际策略/SDK信号→GREEN/门禁/commit；不开部署或profile。
- 修订基础：原批准JA Lombok provided/optional，同Root1.18.46，不下载/升级；复用已有Actuator与Boot primary/unique provider。
- Pseudocode namespace mechanical correction：Core/Spec固定九项中的receipt.age与spool.bytes沿canonical名，不使用F12示例的额外oauth.audit前缀。

| Literal rule | Applicability | Status | Evidence |
| --- | --- | --- | --- |
| Rule 1 | Applicable | Pending final gate | Step7现有Actuator、Core port、命名Supplier/required-args依赖、固定Strategy与原State观察；只两Admin/SDK/批准POM |
| Rule 2 | Applicable | Pending final gate | Step7现有Actuator、Core port、命名Supplier/required-args依赖、固定Strategy与原State观察；只两Admin/SDK/批准POM |
| Rule 3 | Applicable | Pending final gate | Step7现有Actuator、Core port、命名Supplier/required-args依赖、固定Strategy与原State观察；只两Admin/SDK/批准POM |
| Rule 4 | Applicable | Pending final gate | Step7现有Actuator、Core port、命名Supplier/required-args依赖、固定Strategy与原State观察；只两Admin/SDK/批准POM |
| Rule 5 | Applicable | Pending final gate | Step7现有Actuator、Core port、命名Supplier/required-args依赖、固定Strategy与原State观察；只两Admin/SDK/批准POM |
| Rule 6 | Applicable | Pending final gate | Step7现有Actuator、Core port、命名Supplier/required-args依赖、固定Strategy与原State观察；只两Admin/SDK/批准POM |
| Rule 7 | Applicable | Pending final gate | Step7现有Actuator、Core port、命名Supplier/required-args依赖、固定Strategy与原State观察；只两Admin/SDK/批准POM |
| Rule 9 | Applicable | Pending final gate | Step7现有Actuator、Core port、命名Supplier/required-args依赖、固定Strategy与原State观察；只两Admin/SDK/批准POM |
| Rule 10 | Applicable | Pending final gate | Step7现有Actuator、Core port、命名Supplier/required-args依赖、固定Strategy与原State观察；只两Admin/SDK/批准POM |
| Rule 11 | Applicable | Pending final gate | Step7现有Actuator、Core port、命名Supplier/required-args依赖、固定Strategy与原State观察；只两Admin/SDK/批准POM |

| Check ID | Applicability | Status | Evidence |
| --- | --- | --- | --- |
| MC-ARCH-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-REUSE-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-DEP-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-NAME-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-VALID-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-MODEL-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-CONVERT-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-LOG-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-BEAN-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-UTIL-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-JSON-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-TIME-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-CONFIG-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-PATTERN-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-SCOPE-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-TEST-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |
| MC-BLOCKER-001 | Applicable pending diff | Pending final gate | 当前Step复用/预算与Scope已复核，完成后逐行闭合 |

### Step 7 verification — Verified

- Baseline `a415c8aef`；primary Spec/Plan current66-source inventory。
- RED：`/tmp/tq-step7-behavior-red.log` exit1，JA4 behavior failures/0errors；完整reactor按顺序停于JA；独立SA `/tmp/tq-step7-shoubing-red.log`同样4 failures/0errors。最初直接get缺meter造成1 error已修成先assert find，不计有效RED。
- Fresh exact GREEN：§8 Step7 command，`/tmp/tq-step7-commit-green.log` exit0，Starter AutoConfiguration8 + JA metrics8 + SA metrics8 = **24 tests/0fail/error/skip**。Expanded `/tmp/tq-step7-expanded-green.log`69 tests/0fail/error/skip，覆盖Step3/4/6及原consumer；追加DEAD容量/fault两个测试在final exact中通过。
- 新增producer7参由Lombok生成；旧3/6参兼容与旧6参Qualifier metadata保留（旧6参曾是generated public API）；七参真实装配经两Host metrics和SDK factory调用验证。MeterRegistry没有新建生产实例；Startup guard返回技术marker，避免把producer注册成第二alias。
- PC010：delivery owner新转到producer，observer不重复记；配置命名canonical receipt.age/spool.bytes符合Core/Spec，不用Plan简写额外前缀。POM批准版本未升级；只离线controlled测试，未运行任何外部系统/部署。

| Literal rule | Applicability | Status | Diff/path/symbol evidence | Test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | 两Host MetricsComponent/ObservationConfiguration具行为角色；不新增carrier | exact24/expanded69/generated/逐行diff | 原规则保持 | None |
| Rule 2 | Applicable | PASS | Core @Valid record port与Common Default BO约束/shape；host startup校验Props；actual negative signal测试 | exact24/expanded69/generated/逐行diff | 原规则保持 | None |
| Rule 3 | Applicable | PASS | 复用Step5 BO完整Lombok、无新POJO/Converter；两adapter RequiredArgs，原producer兼容6参与新7参 | exact24/expanded69/generated/逐行diff | 原规则保持 | None |
| Rule 4 | Applicable | PASS | 业务Slf4j/具名supplier/recorder/factories/final Qualified；framework MeterRegistry provider选Boot primary/unique，非猜bean名 | exact24/expanded69/generated/逐行diff | 原规则保持 | None |
| Rule 5 | Applicable | PASS | 现Actuator/Micrometer/JDK/Validation；唯一POM是用户批准JA provided Lombok，Root同版无下载 | exact24/expanded69/generated/逐行diff | 原规则保持 | None |
| Rule 6 | Applicable | PASS | BO与Event/receipt/Valid JSON不变；no raw identity/tag；固定receiver/result维度 | exact24/expanded69/generated/逐行diff | 原规则保持 | None |
| Rule 7 | Applicable | PASS | 本Step不动profile；host配置只usage enabled时装配，disabled无secret/registry需求 | exact24/expanded69/generated/逐行diff | 原规则保持 | None |
| Rule 9 | Applicable | PASS | 不可变method-reference Strategy表固定9项；Core Observer与原producer State真实stage/confirm/replay分支 | exact24/expanded69/generated/逐行diff | 原规则保持 | None |
| Rule 10 | Applicable | PASS | Clock/Duration receipt.age非负；真实spool容量snapshot；Timer不包含未进行验证阶段 | exact24/expanded69/generated/逐行diff | 原规则保持 | None |
| Rule 11 | Applicable | PASS | 只两Admin原support/config包及SDK已有audit/auto/observer；无第三层/外平台/SQL | exact24/expanded69/generated/逐行diff | 原规则保持 | None |

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原Core纯port→SDK→两Host Micrometer，零新层与反向host依赖 | 当前Step边界闭合 | None |
| MC-REUSE-001 | Applicable | PASS | 现Boot primary/unique ObjectProvider→named JDK Supplier；现Actuator与Default Validator/producer State复用 | 当前Step边界闭合 | None |
| MC-DEP-001 | Applicable | PASS | JA POM只批准Lombok provided/optional ${lombok.version}=Root1.18.46；无其他依赖/版本/下载 | 当前Step边界闭合 | None |
| MC-NAME-001 | Applicable | PASS | 两个固定host MetricsComponent/Configuration角色明确，不造carrier | 当前Step边界闭合 | None |
| MC-VALID-001 | Applicable | PASS | record入口Common Default/crossfield shape，Props startup schema，非法signal/negative shape抛ConstraintViolation | 当前Step边界闭合 | None |
| MC-MODEL-001 | Applicable | PASS | 无新普通载体；原BO/DTO完整注解，Lombok constructor编译；技术marker不注册producer第二alias | 当前Step边界闭合 | None |
| MC-CONVERT-001 | Not applicable | N/A | N/A：无新业务投影；只技术metric sample→固定策略，Step6 Mapper不变 | 当前Step边界闭合 | None |
| MC-LOG-001 | Applicable | PASS | 业务Slf4j；SDK采集异常一次安全日志，actor/tenant/issuer/token/key不入tags/log | 当前Step边界闭合 | None |
| MC-BEAN-001 | Applicable | PASS | 唯一供应现有primary/unique registry；namedCore recorder/defaultValidator；old6 ctorQualifier metadata保持；缺registry启动FAIL、disabled无依赖 | 当前Step边界闭合 | None |
| MC-UTIL-001 | Applicable | PASS | 只现Micrometer/JDK/Boot/Common，无SDK Micrometer imports或新Utils | 当前Step边界闭合 | None |
| MC-JSON-001 | Applicable | PASS | Metric payload固定BO4字段；旧Event11/receipt6/Valid2保持，labels仅receiver/result有限值 | 当前Step边界闭合 | None |
| MC-TIME-001 | Applicable | PASS | receipt confirmedAt→Clock Duration nonnegative；actual timer与gauge源，原TTL/rotation/expiry不改 | 当前Step边界闭合 | None |
| MC-CONFIG-001 | Applicable | PASS | 两个host同构ConditionalProperty/default false；未执行Step10 profiles/TLS；disabled不额外部署inputs | 当前Step边界闭合 | None |
| MC-PATTERN-001 | Applicable | PASS | fixed immutable method-reference Strategy（Timer/Counter/Gauge）+Observer；producer State真实发出点，去除旧observer重复delivery | 当前Step边界闭合 | None |
| MC-SCOPE-001 | Applicable | PASS | 10 declared paths含PC010必要直接依赖移走duplicate delivery；批准一个JA POM；SQL/其他平台/模板字节未改 | 当前Step边界闭合 | None |
| MC-TEST-001 | Applicable | PASS | 两host各4 expected RED/0error；fresh exact24 passed；expanded69 regression passed；后续追加DEAD/fault分别入exact8+8 | 当前Step边界闭合 | None |
| MC-BLOCKER-001 | Applicable | PASS | 前16项源/controlled检查閉合；runtime负载、实际Registry/exporter/PG/Redis/TLS部署仍Runtime unverified | 当前Step边界闭合 | None |

### Step 7 committed

- Commit `5ecc8228b7a6f96b8b7834f0676d42ef321a9ec5`，`feat(tianquan): bind security metrics to host registries`。exact GREEN24、expanded69与逐项门禁如上；已验证10 committed paths：
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/pom.xml`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityMetricsComponent.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityObservationConfiguration.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityMetricsContractTest.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityMetricsComponent.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityObservationConfiguration.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityMetricsContractTest.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/audit/TokenUsageAuditComponent.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/autoconfigure/IdpStarterAutoConfiguration.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/IdpObservedVerificationComponent.java`

- Step8首次编辑前已记录新HEAD与paths；Spec/Plan仍untracked task docs，其他工作保留；没有runtime/DB/browser/deploy/push动作。

### Step 8 lock — In Progress

- Baseline `5ecc8228b7a6f96b8b7834f0676d42ef321a9ec5`，前置3/4/6/7已Committed；仅本Step四路径；先真实native UserInfo RED，原SAS完整事务/授权/密钥/nonce/CSRF/refresh fixtures复用。
- URI context mechanical inference：MapStruct要求Context类型唯一；native expectedIssuer作为映射source参数，resource仍URI @Context，Java公开签名/可信来源不变，无新类型/框架。

| Literal rule | Applicability | Status | Evidence |
| --- | --- | --- | --- |
| Rule 1 | Applicable | Pending final gate | Step8原Native SAS/Strategy/真实Mapper投影、原一次verify与raw-zero规则、四路径 |
| Rule 2 | Applicable | Pending final gate | Step8原Native SAS/Strategy/真实Mapper投影、原一次verify与raw-zero规则、四路径 |
| Rule 3 | Applicable | Pending final gate | Step8原Native SAS/Strategy/真实Mapper投影、原一次verify与raw-zero规则、四路径 |
| Rule 4 | Applicable | Pending final gate | Step8原Native SAS/Strategy/真实Mapper投影、原一次verify与raw-zero规则、四路径 |
| Rule 5 | Applicable | Pending final gate | Step8原Native SAS/Strategy/真实Mapper投影、原一次verify与raw-zero规则、四路径 |
| Rule 6 | Applicable | Pending final gate | Step8原Native SAS/Strategy/真实Mapper投影、原一次verify与raw-zero规则、四路径 |
| Rule 7 | Applicable | Pending final gate | Step8原Native SAS/Strategy/真实Mapper投影、原一次verify与raw-zero规则、四路径 |
| Rule 9 | Applicable | Pending final gate | Step8原Native SAS/Strategy/真实Mapper投影、原一次verify与raw-zero规则、四路径 |
| Rule 10 | Applicable | Pending final gate | Step8原Native SAS/Strategy/真实Mapper投影、原一次verify与raw-zero规则、四路径 |
| Rule 11 | Applicable | Pending final gate | Step8原Native SAS/Strategy/真实Mapper投影、原一次verify与raw-zero规则、四路径 |

| Check ID | Applicability | Status | Evidence |
| --- | --- | --- | --- |
| MC-ARCH-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-REUSE-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-DEP-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-NAME-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-VALID-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-MODEL-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-CONVERT-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-LOG-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-BEAN-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-UTIL-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-JSON-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-TIME-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-CONFIG-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-PATTERN-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-SCOPE-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-TEST-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |
| MC-BLOCKER-001 | Applicable pending diff | Pending final gate | 原UserInfo Jwt/JWKSource/授权事务/错误族与受控native fixture复用，不启动外部runtime |

### Step1 corrective fixture task — In Progress (Step8 retains its test-only edits)

- Required original Step8 command exposed 4 setup errors in existingOAuthTokenClientCredentialsTest: Step1 CORS now consumes named defaultValidator, old manually-built security fixture did not import Boot ValidationAutoConfiguration. This is a fixture capability correction, not production validation removal.
- Latest user AGENTS authorizes necessary direct dependency files; add exactly one import to this self test and preserve all assertions/scenarios. Separate corrective commit attributed to Step1 per executing skill; Step8 test changes are excluded from it.
- Corrective path: `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthTokenClientCredentialsTest.java`. Source inventory becomes67; no newdependency/POM/SQL/runtime.

### Step1 corrective fixture verification — Verified

- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthTokenClientCredentialsTest.java` only adds existing Boot ValidationAutoConfiguration to test imports. Production zero diff. Prior required gate exposed4 missing-validator setup errors; now `/tmp/tq-step1-corrective-green.log` exit0，client4+CORS5+OAS2=**11 tests/0fail/error/skip**。This is not a behavior RED/GREEN claim; it corrects a required test capability.
- Step8 independent native behavior RED `/tmp/tq-step8-behavior-red.log` has9tests/1failure/0errors (missing usage), remains in progress and excluded from correction commit.

| Literal rule | Applicability | Status | Diff/path/symbol evidence | Test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | One test import only; no production types/models/Bean behavior/profile/time/pattern changes | fresh11 tests/diff | Boot Validator fixture capability restored; ordinary concerns untouched | None |
| Rule 2 | Applicable | PASS | One test import only; no production types/models/Bean behavior/profile/time/pattern changes | fresh11 tests/diff | Boot Validator fixture capability restored; ordinary concerns untouched | None |
| Rule 3 | Not applicable | N/A | One test import only; no production types/models/Bean behavior/profile/time/pattern changes | fresh11 tests/diff | Boot Validator fixture capability restored; ordinary concerns untouched | None |
| Rule 4 | Not applicable | N/A | One test import only; no production types/models/Bean behavior/profile/time/pattern changes | fresh11 tests/diff | Boot Validator fixture capability restored; ordinary concerns untouched | None |
| Rule 5 | Applicable | PASS | One test import only; no production types/models/Bean behavior/profile/time/pattern changes | fresh11 tests/diff | Boot Validator fixture capability restored; ordinary concerns untouched | None |
| Rule 6 | Applicable | PASS | One test import only; no production types/models/Bean behavior/profile/time/pattern changes | fresh11 tests/diff | Boot Validator fixture capability restored; ordinary concerns untouched | None |
| Rule 7 | Not applicable | N/A | One test import only; no production types/models/Bean behavior/profile/time/pattern changes | fresh11 tests/diff | Boot Validator fixture capability restored; ordinary concerns untouched | None |
| Rule 9 | Not applicable | N/A | One test import only; no production types/models/Bean behavior/profile/time/pattern changes | fresh11 tests/diff | Boot Validator fixture capability restored; ordinary concerns untouched | None |
| Rule 10 | Not applicable | N/A | One test import only; no production types/models/Bean behavior/profile/time/pattern changes | fresh11 tests/diff | Boot Validator fixture capability restored; ordinary concerns untouched | None |
| Rule 11 | Applicable | PASS | One test import only; no production types/models/Bean behavior/profile/time/pattern changes | fresh11 tests/diff | Boot Validator fixture capability restored; ordinary concerns untouched | None |

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-REUSE-001 | Applicable | PASS | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-DEP-001 | Applicable | PASS | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-NAME-001 | Applicable | PASS | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-VALID-001 | Applicable | PASS | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-MODEL-001 | Not applicable | N/A | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-CONVERT-001 | Not applicable | N/A | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-LOG-001 | Not applicable | N/A | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-BEAN-001 | Applicable | PASS | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-UTIL-001 | Applicable | PASS | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-JSON-001 | Applicable | PASS | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-TIME-001 | Not applicable | N/A | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-CONFIG-001 | Not applicable | N/A | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-PATTERN-001 | Not applicable | N/A | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-SCOPE-001 | Applicable | PASS | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-TEST-001 | Applicable | PASS | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |
| MC-BLOCKER-001 | Applicable | PASS | Exact one test import; same Boot validation dependency/fixture; no source/schema/JSON/profile changes; focused11 passed | Required fixture corrected, no relaxed assertions | None |

### Step1 corrective fixture committed

- Commit `8ae4ac3a0a8949608e9e250a3b9187e381d74a09`；single committed path `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/controller/OAuthTokenClientCredentialsTest.java`，focused11 passed；Step8 resumes on this HEAD, its only existing edit is metrics native RED test, excluded from corrective commit. No history rewrite or deployment.

### Step 8 verification — Verified

- Effective baseline after corrective task `8ae4ac3a0`；原Step7 dependency `5ecc8228b`保留；只四current paths。
- 有效RED `/tmp/tq-step8-behavior-red.log` exit1：9 tests/1 native-userinfo missing-usage assertion/0errors。最初full command4缺Validator setup错误不算RED，已独立修正。
- Fresh exact GREEN §8 Step8 command `/tmp/tq-step8-commit-green.log` exit0：Persistence16 + OIDC17 + OAS2 + nativeConsent25 + clientCredentials4 + hostMetrics/UserInfo11 = **75 tests/0fail/error/skip**。
- Native secure fullfixture验证：success / missingBearer无login redirect401 / malformed / bad signature / wrongissuer / revoked / reducedScope403 / DB-or-key dependency503 / strict unavailable503；成功userinfo只有原验签once，失败fallback/null refs；strict failure只保留一次already-attempted事实，不再造匿名Event。Metrics异常不改变成功响应。
- Mechanical clarification：MapStruct同方法的两个URI不能都@Context，expectedIssuer为normal source且resource URI唯一Context；公开Java签名、authoritative来源/原算法/provider不变。测试借用package-private native fixtures只在test reflection；生产无reflection或再验证。

| Literal rule | Applicability | Status | Diff/path/symbol evidence | Test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | 原Provider/Configuration/Converter/Test角色保留，新增技术UnavailableException准确 | fresh75/generated/逐行source review | 原规则保持 | None |
| Rule 2 | Applicable | PASS | 原SAS授权/撤销/claims guard结束后Default proof/context/Event校验；Properties正向Default；未经验证的失败不投影refs | fresh75/generated/逐行source review | 原规则保持 | None |
| Rule 3 | Applicable | PASS | 同MapStruct BaseForward Converter复用native Jwt overload，原payload/immutable proof不改；无手动跨模型copy | fresh75/generated/逐行source review | 原规则保持 | None |
| Rule 4 | Applicable | PASS | 原@RequiredArgs/Slf4j Provider；Configuration具名并Slf4j；新factory required dependencies qualified、URI为配置值；无handwritten业务DI | fresh75/generated/逐行source review | 原规则保持 | None |
| Rule 5 | Applicable | PASS | JDK UUID/Duration/Nimbus/原Common/Jackson；零新依赖/框架/生产reflection | fresh75/generated/逐行source review | 原规则保持 | None |
| Rule 6 | Applicable | PASS | 成功原UserInfo JSON、错误原OAuth两个字段/no-store；Event原11字段；失败null refs/无raw输出 | fresh75/generated/逐行source review | 原规则保持 | None |
| Rule 7 | Applicable | PASS | 零profile修改；usage disabled维持原路径；本Step不提前启用其他host | fresh75/generated/逐行source review | 原规则保持 | None |
| Rule 9 | Applicable | PASS | 原GrantTransaction Strategy/TransactionTemplate与JWSVerifier Factory Decorator复用；无第二验证或编解码业务层 | fresh75/generated/逐行source review | 原规则保持 | None |
| Rule 10 | Applicable | PASS | Clock/Instant/monotonic crypto duration；event timestamp/trace受限，原Code/RT绝对期限保持 | fresh75/generated/逐行source review | 原规则保持 | None |
| Rule 11 | Applicable | PASS | 四声明路径原SS Mapper+SA事务provider/config/test；原DB/Outbox与链结构不变 | fresh75/generated/逐行source review | 原规则保持 | None |

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原Admin service.impl/config与SDK mapper方向；无Admin BO进入SDK/新层 | 当前Step受控证明闭合 | None |
| MC-REUSE-001 | Applicable | PASS | 原DefaultJWTProcessor/JWKSource/guards/OidcUserInfoProvider/TransactionTemplate/native fixtures及同一MapStruct+producer key复用 | 当前Step受控证明闭合 | None |
| MC-DEP-001 | Applicable | PASS | POM/依赖/版本/SQL零diff、离线command；仅JDK/原Nimbus能力 | 当前Step受控证明闭合 | None |
| MC-NAME-001 | Applicable | PASS | 原四类型与新nested技术UnavailableException语义清晰 | 当前Step受控证明闭合 | None |
| MC-VALID-001 | Applicable | PASS | guard完成后only verifiedJwt→expectedIssuer proof；Default/cascade源/目标；坏签名/错issuer/revoked/缺scope全匿名且原拒绝 | 当前Step受控证明闭合 | None |
| MC-MODEL-001 | Applicable | PASS | 原Event11、proof immutable record/context class保留，异常非数据carrier | 当前Step受控证明闭合 | None |
| MC-CONVERT-001 | Applicable | PASS | generated toProof(Jwt,expectedIssuer,@Context URI resource,refs)真实生成+BaseForward；expectedIssuer source因Context类型唯一限制，Java签名不变 | 当前Step受控证明闭合 | None |
| MC-LOG-001 | Applicable | PASS | Provider/Config Slf4j、crypto metric错误仅failureType且一次；不记录token/jti/key/code/subject | 当前Step受控证明闭合 | None |
| MC-BEAN-001 | Applicable | PASS | new Optional providers qualified仅enabled强依赖；原tenant decoder/chain Bean名字、不同grant provider/ctor不变 | 当前Step受控证明闭合 | None |
| MC-UTIL-001 | Applicable | PASS | JDK+原组件/Nimbus无通用Utils或新JSON，reflection仅借用package-private测试fixture | 当前Step受控证明闭合 | None |
| MC-JSON-001 | Applicable | PASS | NativeSuccess JSON原状/匿名错误/no-store/WWW-Authenticate；坏签名/撤销/缺Token/503 zero identity实际断言 | 当前Step受控证明闭合 | None |
| MC-TIME-001 | Applicable | PASS | actual verify only timer（native success count=1），Clock timestamp；nonce/CSRF/RT期限原25 native回归 | 当前Step受控证明闭合 | None |
| MC-CONFIG-001 | Applicable | PASS | 4 source paths、无profile/TLS变更，enabled部署inputs由既有typed props，disabled兼容 | 当前Step受控证明闭合 | None |
| MC-PATTERN-001 | Applicable | PASS | 原Strategy在guards后投递、原事务Decorator；VerifierFactory/JWSVerifier全能力委托，不再decode | 当前Step受控证明闭合 | None |
| MC-SCOPE-001 | Applicable | PASS | 原4paths；独立Step1 fixture correction先提交8ae4ac3a0，未混进当前源码commit；其他平台/SQL字节不变 | 当前Step受控证明闭合 | None |
| MC-TEST-001 | Applicable | PASS | effective RED9/1 behaviorfail/0error；fresh exact75/0fail/error/skip；包括实际S256/nonce/CSRF/RT/Basic/nativeUserInfo/strict/metricsfail | 当前Step受控证明闭合 | None |
| MC-BLOCKER-001 | Applicable | PASS | 所有源/controlled门禁闭合；PG/Redis/真实多进程/TLS/browser/p95仍Runtime unverified，未擅启服务 | 当前Step受控证明闭合 | None |

### Step 8 committed

- Commit `1e311c28957eaea6d80be2bc94660beff446f620`，message `fix(shoubing): audit native UserInfo token use`；fresh exact75/4 committed paths已核对：
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/config/AuthorizationServerConfiguration.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/oauth/service/impl/TransactionalOAuthAuthenticationProvider.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityMetricsContractTest.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter/src/main/java/top/egon/cola/platform/tianquan/shoubing/starter/security/domain/converter/TokenUsageAuditEventConverter.java`

- Step9首个源码编辑前已记录此HEAD/paths；仅Spec/Plan任务文档仍untracked，其他工作保留；runtime/DB/browser/deploy/push未执行。

### Step 9 lock — In Progress

- Baseline `1e311c28957eaea6d80be2bc94660beff446f620`，依赖2/6/7已Committed；仅8声明路径，原SPI五方法/错误族/Bean替代语义和两chain顺序复核。先contract scaffold→真实PDP/chain RED→Decorator/bridge/render装配→完整gate/commit；无外部runtime。

| Literal rule | Applicability | Status | Evidence |
| --- | --- | --- | --- |
| Rule 1 | Applicable | Pending final gate | Step9原Default PDP/method/field/context/chain与Core port/strict producer复用，权限结果不改 |
| Rule 2 | Applicable | Pending final gate | Step9原Default PDP/method/field/context/chain与Core port/strict producer复用，权限结果不改 |
| Rule 3 | Applicable | Pending final gate | Step9原Default PDP/method/field/context/chain与Core port/strict producer复用，权限结果不改 |
| Rule 4 | Applicable | Pending final gate | Step9原Default PDP/method/field/context/chain与Core port/strict producer复用，权限结果不改 |
| Rule 5 | Applicable | Pending final gate | Step9原Default PDP/method/field/context/chain与Core port/strict producer复用，权限结果不改 |
| Rule 6 | Applicable | Pending final gate | Step9原Default PDP/method/field/context/chain与Core port/strict producer复用，权限结果不改 |
| Rule 7 | Applicable | Pending final gate | Step9原Default PDP/method/field/context/chain与Core port/strict producer复用，权限结果不改 |
| Rule 9 | Applicable | Pending final gate | Step9原Default PDP/method/field/context/chain与Core port/strict producer复用，权限结果不改 |
| Rule 10 | Applicable | Pending final gate | Step9原Default PDP/method/field/context/chain与Core port/strict producer复用，权限结果不改 |
| Rule 11 | Applicable | Pending final gate | Step9原Default PDP/method/field/context/chain与Core port/strict producer复用，权限结果不改 |

| Check ID | Applicability | Status | Evidence |
| --- | --- | --- | --- |
| MC-ARCH-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-REUSE-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-DEP-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-NAME-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-VALID-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-MODEL-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-CONVERT-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-LOG-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-BEAN-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-UTIL-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-JSON-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-TIME-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-CONFIG-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-PATTERN-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-SCOPE-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-TEST-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |
| MC-BLOCKER-001 | Applicable pending diff | Pending final gate | 原结构/具名PDP组合、前置Code/Proof保证已复核；源码完成后逐行gate |


### Step 9 final gate — Verified

- Effective RED: `/tmp/tq-step9-red.log`，真实 JA chain 缺 Token 返回 403 而非 401、真实 PDP DENY 缺 meter，2 behavior failures/0 errors。最终扩展命令在原选择器追加 `Rbac3StarterAutoConfigurationTest`；`/tmp/tq-step9-final2.log` exit 0，44 tests（Starter 15、JA 18、SA 11），0 failures/errors/skips。
- 新启用过滤器测试揭示本 Step 注册顺序错误（`/tmp/tq-step9-final.log`，3 context errors）；修正为先向 HttpSecurity 注册 Idp 顺序，再放置 observation before Idp，重新 GREEN。此中间错误不计 RED。
- JA tenant filter 置于 authenticated URL gate 后，仍位于业务执行之前；否则缺 Token 先落入 tenant 错误族而非指定 401。Idp/RBAC/tenant 的相对身份处理顺序不变；两链 missing、invalid、snapshot denied/unavailable、proof/scopes、审计失败及健康路径均有受控测试。
- Decorator 的五个原 PDP 方法结果未改变；无 Token 仅指标，有 proof 才记录拒绝；RequestContextHolder 原请求标记防止 handler 二次投递。metrics failure 保留原 exception instance；严格审计失败升级原 RuntimeUnavailable family。实际 Starter 默认组合与自定义 SPI override 均通过。
- No POM/SQL/profile/frontend/其他平台变化；runtime/PG/Redis/TLS/部署仍未执行。

| Literal rule | Applicability | Status | Diff/path/symbol evidence | Validation | Finding | Action |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | 新增 Rbac3ObservedAuthorizationService 是行为 Decorator；其余 Filter/Handler/Configuration 均原角色命名 | 实际编译，无新载体 | No unresolved finding | None |
| Rule 2 | Applicable | PASS | 五方法使用原 Request constraints 与 Validator；结果验证、SDK proof/Event 仍经原约束 | 五方法真实 Default PDP 与 strict failure 测试 | No unresolved finding | None |
| Rule 3 | Not applicable | N/A | 无新模型/转换器；复用既有 immutable proof 与生成 Event Converter | 未添加 carrier 或 manual converter | No unresolved finding | None |
| Rule 4 | Applicable | PASS | Decorator/Filter/Handler @Slf4j；Decorator qualified final Lombok；Filter Lombok private ctor+旧 public adapter；新具名 Bean/qualified provider | 真实 Starter 默认/override 与启用链装配 | No unresolved finding | None |
| Rule 5 | Applicable | PASS | JDK Supplier/Function/AtomicBoolean 与原 ValidationUtils；未引入依赖或 Utils | imports/POM diff 检查 | No unresolved finding | None |
| Rule 6 | Applicable | PASS | 原 Boot Jackson renderer；两 host 原错误族保留，no-store/WWW header | 真实 HTTP 401/403/503 与字段错误 body | No unresolved finding | None |
| Rule 7 | Not applicable | N/A | 此 Step 无 YAML/property 新增或改值 | 配置文件不在8文件 diff | No unresolved finding | None |
| Rule 9 | Applicable | PASS | 批准 Decorator 包装原 PDP，方法引用投影；原 filter chain/callback 复用 | 五方法/去重/原异常及失败闭合 | No unresolved finding | None |
| Rule 10 | Applicable | PASS | 生产本 Step 无新增日期 API；事件时间沿用观察组件 Clock | 测试 Instant/Clock.fixed；无 util.Date | No unresolved finding | None |
| Rule 11 | Applicable | PASS | 原 Admin/Starter 边界保持；原严格 Redis Streams usage CQE；零DDL | 8声明路径 diff、44受控测试 | No unresolved finding | None |

| Check ID | Applicability | Status | Evidence | Finding | Action |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原结构保持; Rule 11 evidence above | No unresolved Step finding | None |
| MC-REUSE-001 | Applicable | PASS | 复用 SDK proof/observer、原 Default PDP、原 Spring链; Rule 9 evidence above | No unresolved Step finding | None |
| MC-DEP-001 | Applicable | PASS | 无依赖变化; Rule 5 evidence above | No unresolved Step finding | None |
| MC-NAME-001 | Applicable | PASS | 行为角色清晰; Rule 1 evidence above | No unresolved Step finding | None |
| MC-VALID-001 | Applicable | PASS | 输入/结果/事件边界验证; Rule 2 evidence above | No unresolved Step finding | None |
| MC-MODEL-001 | Not applicable | N/A | 无新模型; Rule 3 evidence above | No unresolved Step finding | None |
| MC-CONVERT-001 | Not applicable | N/A | 本Step未改 Converter; Rule 3 evidence above | No unresolved Step finding | None |
| MC-LOG-001 | Applicable | PASS | 固定 failureType 单次日志不含 token; Rule 4 evidence above | No unresolved Step finding | None |
| MC-BEAN-001 | Applicable | PASS | 具名新组合、真实 primary/override 验证; Rule 4 evidence above | No unresolved Step finding | None |
| MC-UTIL-001 | Applicable | PASS | 工具边界保持; Rule 5 evidence above | No unresolved Step finding | None |
| MC-JSON-001 | Applicable | PASS | 原错误族与 no-store; Rule 6 evidence above | No unresolved Step finding | None |
| MC-TIME-001 | Applicable | PASS | 原 Clock/Instant; Rule 10 evidence above | No unresolved Step finding | None |
| MC-CONFIG-001 | Not applicable | N/A | 无 profile 修改; Rule 7 evidence above | No unresolved Step finding | None |
| MC-PATTERN-001 | Applicable | PASS | Decorator/chain 有实际参与者; Rule 9 evidence above | No unresolved Step finding | None |
| MC-SCOPE-001 | Applicable | PASS | 仅8声明路径; Rule 11 evidence above | No unresolved Step finding | None |
| MC-TEST-001 | Applicable | PASS | 44项实际 GREEN、strict Plan validator、diff --check; Rule 11 evidence above | No unresolved Step finding | None |
| MC-BLOCKER-001 | Applicable | PASS | 逐项复核，无当前 Step 未闭合项；部署验收另列; Rule 11 evidence above | No unresolved Step finding | None |

### Step 9 — Committed

- Commit `62ad8498a856a4014276cd593299cba2b9a48702`; 8 Step paths verified with git show. All gates above closed before commit; Step 10 baseline is this HEAD.
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/Rbac3AdminSecurityChainTest.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityMetricsContractTest.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/authorization/Rbac3ObservedAuthorizationService.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/security/Rbac3BearerAuthenticationFilter.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/web/Rbac3AuthorizationExceptionHandler.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter/src/main/java/top/egon/cola/platform/tianquan/jianshen/starter/autoconfigure/Rbac3StarterAutoConfiguration.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/Rbac3AdminSecurityConfiguration.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/IdpSecurityConfig.java`


### Step 10 lock — In Progress

- Baseline `62ad8498a`；Step 1/2/4/6/7/8/9 均已有提交。仅原10路径及 PC-011 两个必要自身 readiness factory；不改共享组件或他平台。测试先增加真实 YAML 加载/错误 TLS 输入 RED，再配置与 qualified readiness 组合 GREEN。卷/key/lease仅临时目录；不启动服务。
- PC-011（依据最新用户 AGENTS 必要正确性授权）：现有 publication/readiness 并不会消费观察配置的检查。纳入 SA `support/runtime/IdpPlatformConfiguration.java` 的 `idpRuntimeReadiness` 与 JA `config/runtime/Rbac3PlatformIntegrationConfiguration.java` 的 `rbac3ReadinessIndicator`；只用 qualified optional BooleanSupplier 组合一个额外 guard，disabled 回原逻辑，不覆盖原失败条件；不改 readiness 返回协议/接口/DDL。
- Rule 1: Applicable, Pending final gate；现有技术配置/属性/JDK卷lease、原readiness组合；完成后逐项评价。
- Rule 2: Applicable, Pending final gate；现有技术配置/属性/JDK卷lease、原readiness组合；完成后逐项评价。
- Rule 3: Applicable, Pending final gate；现有技术配置/属性/JDK卷lease、原readiness组合；完成后逐项评价。
- Rule 4: Applicable, Pending final gate；现有技术配置/属性/JDK卷lease、原readiness组合；完成后逐项评价。
- Rule 5: Applicable, Pending final gate；现有技术配置/属性/JDK卷lease、原readiness组合；完成后逐项评价。
- Rule 6: Applicable, Pending final gate；现有技术配置/属性/JDK卷lease、原readiness组合；完成后逐项评价。
- Rule 7: Applicable, Pending final gate；现有技术配置/属性/JDK卷lease、原readiness组合；完成后逐项评价。
- Rule 9: Applicable, Pending final gate；现有技术配置/属性/JDK卷lease、原readiness组合；完成后逐项评价。
- Rule 10: Applicable, Pending final gate；现有技术配置/属性/JDK卷lease、原readiness组合；完成后逐项评价。
- Rule 11: Applicable, Pending final gate；现有技术配置/属性/JDK卷lease、原readiness组合；完成后逐项评价。
- MC-ARCH-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-REUSE-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-DEP-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-NAME-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-VALID-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-MODEL-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-CONVERT-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-LOG-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-BEAN-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-UTIL-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-JSON-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-TIME-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-CONFIG-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-PATTERN-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-SCOPE-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-TEST-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。
- MC-BLOCKER-001: Pending final gate；qualified组合与 profiles/schema/卷失败负例在本Step闭合。


### Step 10 final gate — Verified

- RED `/tmp/tq-step10-red.log`: 2 expected behavior failures/0 errors，JA production HTTP被接受、profile缺核心键。GREEN `/tmp/tq-step10-final.log`: exit 0，51 tests（SDK 8、JA 20、SA 23），0 failures/errors/skips；包含真实 `SpringConstructorResolutionTest` 8项。中间一次新 YAML重复management被真实loader识别并已修正，不计behavior RED。
- 6个自身profile经YamlPropertySourceLoader+Binder+真实Validator证明usage/delivery值与核心键同构；offline/local-it关闭，base开启且RPC/Tianshu TLS默认true，local三重例外与loopback endpoint约束；issuer总为HTTPS，fallback必须当前trusted issuer。
- host provider沿 Boot unique/primary Registry，不造第二 registry；producer/key/Validator缺失拒绝开启。JDK lease持有到context.close；同目录第二context失败、关闭后可再获取，DEAD/未完成严格handoff/满容量/key权限失败动态readiness false。原publication/readiness额外AND guard已编译，不覆盖旧检查，真实部署仍unverified。
- PC-011仅两自身既有配置factory必要依赖，零新公共协议/框架/库/SQL；Strict Plan validator PASS，12路径 diff --check PASS。

| Literal rule | Applicability | Status | Diff evidence | Validation evidence | Finding | Action |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | 原 Configuration/Properties 与 HealthIndicator/JDK FileChannel，无新业务载体 | 51项编译测试 | No unresolved Step finding | None |
| Rule 2 | Applicable | PASS | startup真实Validator验证usage/audit；Binder绑定原Idp/client配置，URI/TLS边界校验 | profile binder、plaintext/local/production负例 | No unresolved Step finding | None |
| Rule 3 | Not applicable | N/A | 没有新增模型或Converter；immutable配置与既有POJO不变 | 12路径inventory | No unresolved Step finding | None |
| Rule 4 | Applicable | PASS | 无新增业务注入类；技术factory新参数/guard/lease明确Bean名与Qualifier；原type-based兼容参数未改 | actual host ContextRunner和constructor resolution | No unresolved Step finding | None |
| Rule 5 | Applicable | PASS | JDK Files/FileChannel/BooleanSupplier 与原Boot Binder/ValidationUtils | 无POM/Utils修改 | No unresolved Step finding | None |
| Rule 6 | Applicable | PASS | 无外部JSON变化；配置由Boot YAML/Binder，Health原框架协议 | 真实YAML加载无重复键，原Jackson保持 | No unresolved Step finding | None |
| Rule 7 | Applicable | PASS | 6profile核心usage3/delivery3/RPC2/Tianshu2键一致；base/local/offline值独立 | YamlPropertySourceLoader/Binder/Validator断言 | No unresolved Step finding | None |
| Rule 9 | Applicable | PASS | 复用Specification风格BooleanSupplier guard与原readiness组合，技术lease生命周期由Spring拥有 | 真实lease冲突/释放、guard false不解除原状态 | No unresolved Step finding | None |
| Rule 10 | Applicable | PASS | delivery沿原Duration，原timing/Clock不变 | PT1H/PT30S/100绑定断言 | No unresolved Step finding | None |
| Rule 11 | Applicable | PASS | 12 own Admin路径；CQE依前置strict MQ/receipt不变；无DDL/tenant查询变化 | path diff、51受控测试、零其他platform变化 | No unresolved Step finding | None |

| Check ID | Applicability | Status | Evidence | Finding | Action |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原Admin/配置角色; Rule 11 evidence | No unresolved Step finding | None |
| MC-REUSE-001 | Applicable | PASS | 原Boot registry/Binder、JDK卷lease、原readiness聚合; Rule 9 evidence | No unresolved Step finding | None |
| MC-DEP-001 | Applicable | PASS | 没有库/POM改动; Rule 5 evidence | No unresolved Step finding | None |
| MC-NAME-001 | Applicable | PASS | 无新载体；明确技术Bean角色; Rule 1 evidence | No unresolved Step finding | None |
| MC-VALID-001 | Applicable | PASS | 原constraints+边界URI/TLS校验; Rule 2 evidence | No unresolved Step finding | None |
| MC-MODEL-001 | Not applicable | N/A | 未新增模型; Rule 3 evidence | No unresolved Step finding | None |
| MC-CONVERT-001 | Not applicable | N/A | 未改转换器; Rule 3 evidence | No unresolved Step finding | None |
| MC-LOG-001 | Applicable | PASS | 仅technical配置，无新增敏感日志；Health失败固定代码; Rule 4 evidence | No unresolved Step finding | None |
| MC-BEAN-001 | Applicable | PASS | qualified新Bean依赖与实际context/ctor测试; Rule 4 evidence | No unresolved Step finding | None |
| MC-UTIL-001 | Applicable | PASS | 仅JDK/原Boot/Egon工具; Rule 5 evidence | No unresolved Step finding | None |
| MC-JSON-001 | Applicable | PASS | 协议未变；真实loader/Binder; Rule 6 evidence | No unresolved Step finding | None |
| MC-TIME-001 | Applicable | PASS | 原Duration/Clock contract; Rule 10 evidence | No unresolved Step finding | None |
| MC-CONFIG-001 | Applicable | PASS | 六profile真实绑定与叶子同构; Rule 7 evidence | No unresolved Step finding | None |
| MC-PATTERN-001 | Applicable | PASS | Guard组合和原状态生命周期; Rule 9 evidence | No unresolved Step finding | None |
| MC-SCOPE-001 | Applicable | PASS | 10原文件+2 PC011必要factory; Rule 11 evidence | No unresolved Step finding | None |
| MC-TEST-001 | Applicable | PASS | 51 tests fresh exit0，RED2/0，strict validator/diffcheck; Rule 11 evidence | No unresolved Step finding | None |
| MC-BLOCKER-001 | Applicable | PASS | 本Step门禁全部闭合；真实发布/卷/TLS验收仍另列; Rule 11 evidence | No unresolved Step finding | None |

### Step 10 — Committed

- Commit `4ef9b3856ee623180458855de7f23607c6ba817b`; 12 declared/PC011 source paths verified. Step 11 baseline is this HEAD; 51 fresh tests and all gate rows above passed before commit.
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityMetricsContractTest.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityMetricsContractTest.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/security/TianquanSecurityObservationConfiguration.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/security/TianquanSecurityObservationConfiguration.java`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application.yml`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-local.yml`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/resources/application-oauth-migration.yml`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/application.yml`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/resources/application-local.yml`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/resources/application-local-it.yml`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java/top/egon/cola/platform/tianquan/shoubing/admin/support/runtime/IdpPlatformConfiguration.java`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java/top/egon/cola/platform/tianquan/jianshen/admin/config/runtime/Rbac3PlatformIntegrationConfiguration.java`


### Step 11 preflight — Blocked; no source mutation or completion commit

- Baseline/current HEAD `4ef9b3856`。真实 TypeScript `resolveModuleName`（Bundler resolution，无paths alias）证明两个host实际都解析旧 `@egon-cola/admin-web-shared` 0.2.3：
  - SW: `egon-cola-tianquan-shoubing-admin-web/node_modules/@egon-cola/xingyuan-admin-web-shared/dist/index.d.ts`。
  - JW: 当前host node_modules未安装，解析父目录 `egon-cola-tianquan-jianshen/node_modules/@egon-cola/xingyuan-admin-web-shared/dist/index.d.ts`。
- 实际旧 d.ts：`login(request: GatewayLoginRequest)`、`logout(): Promise<void>`、`stepUp(password: string)`；仓库共享源package.json为0.2.4，版本声明/源文件不能证明两个host已有匹配制品。SW lock仍为0.2.3，未伪造resolved/integrity。
- 两个普通 `npm --prefix ... run typecheck` 均实际失败：SW exit1，TS2554 login少参数、TS2339 remoteState不在void；JW exit2，同错误并包括stepUp旧参数及测试stub类型不匹配。日志 `/tmp/tq-step11-shoubing-typecheck.log`、`/tmp/tq-step11-jianshen-typecheck.log`。这是依赖输入失败，不计behavior RED。
- Plan Step11 File3明确：“先由既有制品流程提供当前已提交0.2.4的可信resolution/integrity及已安装包；不在本轮下载/发布。”技能也要求缺少必需文件/符号时停止。因此不下载、安装、发布、改node_modules源码、改共享包命名/源码或添加临时alias，不创建不能GREEN的两个新test，不产生空提交。
- 必要输入：按既有可信制品流程提供与已批准源码0.2.4相符且两个当前声明可解析的制品/metadata，并在两host安装。随后恢复Step11 contract RED/GREEN、各自全suite/typecheck及正式最终Spec audit。若需要改变“不在本轮下载/发布”约束，必须先明确修订；不会自行扩到其他平台。

### 阶段交付验证（不替代最终 Spec conformance audit）

- 依Plan §8 exact最终受影响模块/跨模块选择器，离线且不激活integration profile：`/tmp/tq-final-backend.log` exit0，28个实际 test classes、255 tests，0 failures/errors/skips；包括 `ResourceServerPolicyTest` Core3项。此前 commentary 252仅模块汇总遗漏Core3项，此记录以逐类实际报告255为准。
- `git diff 214e53fd1^ HEAD --name-only`：66 committed paths，全部仅授柄/鉴神自身；无SQL/DDL/模板/其他平台/前端源修改。源工作树 clean，只有本次任务Spec/Plan两文档untracked。未push/PR/启动应用/数据库/Redis/浏览器/部署。
- 暂存区为空；每Step一个提交，另有归属Step1的必要native fixture corrective commit，历史未重写。
- 本次新scratch Python orchestration scripts均位于/tmp并删除；可重复行为已进入各自身JUnit测试。仅保留/tmp验证log作证据，未在repo scripts/work留下本任务未归属脚本；已有其他文件保留。无Maven/npm进程遗留。

| Step | Status | Commit | Validation / paths |
| --- | --- | --- | --- |
| 1 | Committed | 214e53fd1 | OAuth CORS/OpenAPI；前文 exact7 paths与RED/GREEN记录 |
| 2 | Committed | 26f77b7c2 | 显式字段写权限与原PDP；前文exact10 paths |
| 3 | Committed | cb7539696 | Strict durable handoff；前文exact2 paths |
| 4 | Committed | dcf282aa7 | 跨节点签名回执/重放；前文exact8 paths |
| 5 | Committed | 8a4a30c82 | 真正密码学计时/缓存/acquire；前文exact8 paths |
| 6 | Committed | a415c8aef | HTTP/RPC proof/观察；前文exact15 paths |
| 7 | Committed | 5ecc8228b | Host registry/delivery metrics；前文exact10 paths |
| 1 corrective | Committed | 8ae4ac3a0 | 原native fixture Validation装配，11测试；exact1 path |
| 8 | Committed | 1e311c289 | 原生UserInfo；75测试/exact4 paths |
| 9 | Committed | 62ad8498a | PDP proof/错误/去重；44测试/exact8 paths |
| 10 | Committed | 4ef9b3856 | own profiles/TLS/readiness；51测试/exact12 paths |
| 11 | Blocked | None | 两host实际共享产物0.2.3；typecheck均失败；无source/test/lock mutation |

| Requirement | Stage status | Evidence / remaining action |
| --- | --- | --- |
| REQ-001 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-002 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-003 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-004 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-005 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-006 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-007 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-008 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-009 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-010 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-011 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-012 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-013 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-014 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-015 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-016 | Partial / Runtime unverified | Step10受控transport/readiness已证；真实TLS/卷/PG/Redis/自身浏览器/负载验收未运行，不能供其他平台接入或宣称生产Ready |
| REQ-017 | Not satisfied | Step11制品输入阻塞，真实前端contract/fullsuite/typecheck未通过 |
| REQ-018 | Source verified; final audit pending | 对应已提交Steps与255后端受控测试；不得据此替代正式完整Spec audit或需要的部署验收 |
| REQ-019 | Partial / Runtime unverified | Step10受控transport/readiness已证；真实TLS/卷/PG/Redis/自身浏览器/负载验收未运行，不能供其他平台接入或宣称生产Ready |

Step11 Literal Rules及Manual Check当前门禁（非最终全树门禁）：

| Literal rule | Applicability | Status | Evidence | Finding / action |
| --- | --- | --- | --- | --- |
| Rule 1 | Not applicable | N/A | 本Step尚无源码编辑，Java模型/注入/日期等不涉及 | 需要可信0.2.4输入后重跑Step11 |
| Rule 2 | Not applicable | N/A | 本Step尚无源码编辑，Java模型/注入/日期等不涉及 | 需要可信0.2.4输入后重跑Step11 |
| Rule 3 | Not applicable | N/A | 本Step尚无源码编辑，Java模型/注入/日期等不涉及 | 需要可信0.2.4输入后重跑Step11 |
| Rule 4 | Not applicable | N/A | 本Step尚无源码编辑，Java模型/注入/日期等不涉及 | 需要可信0.2.4输入后重跑Step11 |
| Rule 5 | Not applicable | N/A | 本Step尚无源码编辑，Java模型/注入/日期等不涉及 | 需要可信0.2.4输入后重跑Step11 |
| Rule 6 | Applicable | BLOCKED | 真实package/lock/声明与预期0.2.4 contract不一致；禁止伪造metadata或alias | 需要可信0.2.4输入后重跑Step11 |
| Rule 7 | Applicable | BLOCKED | 真实package/lock/声明与预期0.2.4 contract不一致；禁止伪造metadata或alias | 需要可信0.2.4输入后重跑Step11 |
| Rule 9 | Not applicable | N/A | 本Step尚无源码编辑，Java模型/注入/日期等不涉及 | 需要可信0.2.4输入后重跑Step11 |
| Rule 10 | Not applicable | N/A | 本Step尚无源码编辑，Java模型/注入/日期等不涉及 | 需要可信0.2.4输入后重跑Step11 |
| Rule 11 | Applicable | PASS | preflight只读，两个自身owner与声明保持；其他平台未改 | 需要可信0.2.4输入后重跑Step11 |

| Check ID | Applicability | Status | Evidence | Finding / action |
| --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 只读preflight，66 source paths范围验证；无Step11编辑 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-REUSE-001 | Applicable | BLOCKED | 实际旧artifact/lock与两host typecheck失败；需要可信0.2.4输入 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-DEP-001 | Applicable | BLOCKED | 实际旧artifact/lock与两host typecheck失败；需要可信0.2.4输入 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-NAME-001 | Not applicable | N/A | 此Step尚无source mutation，未改变对应生产合同 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-VALID-001 | Not applicable | N/A | 此Step尚无source mutation，未改变对应生产合同 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-MODEL-001 | Not applicable | N/A | 此Step尚无source mutation，未改变对应生产合同 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-CONVERT-001 | Not applicable | N/A | 此Step尚无source mutation，未改变对应生产合同 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-LOG-001 | Not applicable | N/A | 此Step尚无source mutation，未改变对应生产合同 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-BEAN-001 | Not applicable | N/A | 此Step尚无source mutation，未改变对应生产合同 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-UTIL-001 | Not applicable | N/A | 此Step尚无source mutation，未改变对应生产合同 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-JSON-001 | Applicable | BLOCKED | 实际旧artifact/lock与两host typecheck失败；需要可信0.2.4输入 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-TIME-001 | Not applicable | N/A | 此Step尚无source mutation，未改变对应生产合同 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-CONFIG-001 | Applicable | BLOCKED | 实际旧artifact/lock与两host typecheck失败；需要可信0.2.4输入 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-PATTERN-001 | Not applicable | N/A | 此Step尚无source mutation，未改变对应生产合同 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-SCOPE-001 | Applicable | PASS | 只读preflight，66 source paths范围验证；无Step11编辑 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-TEST-001 | Applicable | BLOCKED | 实际旧artifact/lock与两host typecheck失败；需要可信0.2.4输入 | 不冒充GREEN/完成提交；输入后重跑 |
| MC-BLOCKER-001 | Applicable | BLOCKED | 实际旧artifact/lock与两host typecheck失败；需要可信0.2.4输入 | 不冒充GREEN/完成提交；输入后重跑 |

当前 verdict：`BLOCKED — Step11 front-end artifact input unavailable`；后端阶段255项测试通过；正式最终审计未开始，生产Runtime验收未完成。


### PC-012 / Step 11 resume — In Progress（2026-10-11）

- 用户已明确授权按现有制品流程从npmjs获取安装可信0.2.4，解除历史下载/安装输入阻塞；不授权发布/服务/部署/其他平台接入。历史Blocked记录作为当时证据保留，本节取代其当前状态。
- Baseline `4ef9b3856ee623180458855de7f23607c6ba817b`，Steps1–10已提交，工作树仅本任务Spec/Plan文档，暂存区为空。源码范围原3路径+必要SW package.json，共4路径；全局库存70 unique。
- registry @egon-cola/xingyuan-admin-web-shared返回404；实际 @egon-cola/admin-web-shared@0.2.4存在。复用JW既有alias到SW；实际tarball https://registry.npmjs.org/@egon-cola/admin-web-shared/-/admin-web-shared-0.2.4.tgz。
- dist.integrity `sha512-iGM7nfou3WmEjnPsAgYvXScnQ1eDkQYHnJlStcgQaBH/OfU65PFVtqm6Usi31oByXE5P9rkBMmlduJw5GMvXnA==`；SHA1 `545ad24e4bdfd2bb0bb15ed2b70f6d8383d387ec`。下载后实际字节核对，认证contract比对源码；version alone不是可信依据。
- 文件顺序：创建SW/JW真实contract测试→旧0.2.3行为RED（不计收集/编译失败）→核验获取artifact→SW root alias/lock必要metadata→两host精确0.2.4安装（ignore-scripts、no audit/no fund、legacy-peer-deps；JW no-save/no-lock）→真实installed GREEN、两host普通typecheck/fullsuite→逐项gate→四路径一次Step提交。
- 10 Literal Rules与17 MC IDs沿Step gate逐项重新执行；Java-only模型/DI等N/A需以无Java修改证明，Scope/Test始终Applicable。之后独立最终Spec audit，运行受控后端/静态门禁；所有真实runtime验收继续Runtime unverified。


### Step 11 artifact verification — Blocked（2026-10-11 新证据）

- 已按用户最新授权修订并执行获取安装；不再沿用历史“尚未允许下载”作为阻塞理由。本次失败是具体已发布制品内容不匹配，以及JW其他现有依赖访问404。
- 从npmjs获取实际 `@egon-cola/admin-web-shared@0.2.4` tarball（873630 bytes），SHA512/SHA1与registry dist匹配；按registry key API `https://registry.npmjs.org/-/npm/v1/keys` 验证 `name@version:dist.integrity` 的ECDSA签名 PASS。结果 `/tmp/tq11-artifact/trust-result.json`，metadata/pack与原tarball保留；包来源可信不等于认证契约一致。
- 发布内容（声明与JS AST实际解析）仍为旧协议：
  - `login(request: GatewayLoginRequest): Promise<GatewayLoginResult>`，实际 POST `/oauth2/login`、读取tenantId/username/password。
  - `logout(): Promise<void>`，实际 POST `/oauth2/logout`，未返回remoteState。
  - `stepUp(password: string): Promise<void>`，实际 POST `/oauth2/step-up`，body.password。
  - 发布options无navigate/registrationId。对照仓库已批准 `egon-cola-xingyuan-admin-web-shared/src/auth/gatewayAuthClient.ts`：login()/stepUp()只导航Edge，logout返回CONFIRMED/PENDING/UNKNOWN，CSRF后POST `/bff/logout`。这是内容不符合，不是类型文件单独陈旧。
- 两个真实installed contract测试已创建，未mock共享模块/未设源码alias。旧0.2.3 RED：SW与JW各5实际assertion failures，正常collect/import，无setup异常；日志 `/tmp/tq11-sw-red.log`、`/tmp/tq11-jw-red.log`。
- SW按原安装工具 `npm install --legacy-peer-deps` 的必要identity修正为既有JW npm alias，范围仍^0.2.0；现有lock仅root该dependency与共享节点5字段修改，其他lock节点diff=0。精确0.2.4安装exit0；采用no-save/no-lock/ignore-scripts/no-audit/no-fund，不发布、不触发生命周期脚本。npm重整了本host的ignored node_modules（12 added/9 removed/106 changed）；没有更改其他依赖的源码声明或lock节点，没有修改其他host/parent node_modules。
- SW实际0.2.4重新运行contract仍5/5 failures（exit1），日志 `/tmp/tq11-sw-024-contract.log`；普通typecheck exit1，生产AuthContext依旧TS2554(login参数)/TS2339(remoteState)，新增contract也因旧options/参数失败，日志 `/tmp/tq11-sw-024-typecheck.log`。不能把这个结果变成GREEN或让平台退回密码登录协议。
- JW常规精确安装exit1，当前registry对既有 `@egon-cola/tianquan-jianshen-react-sdk@0.1.1` 访问返回404，日志 `/tmp/tq11-jw-install.log`；该host未形成own shared0.2.4安装，不能声称其contract已验证新包。不自动改该SDK声明、发布SDK、复用源码alias或手改node_modules内容绕过安装。
- 当前源码变更只有Step11四声明路径：两个新contract tests、SW package.json和package-lock；未提交失败的Step、未创建空commit。Spec/Plan文档仍任务-owned untracked；所有既有Steps1–10提交保持。
- 已提出缺失信息问题：包含新OIDC导航/remoteState合同的准确0.2.4 npm包名/链接。不能假设用户说“已发布”就跳过字节/真实契约核验。若当前admin-web-shared的0.2.4就是指定制品，需要由共享包既有发布流程提供正确构建的新制品/版本后再明确修订本Step版本输入（npm既有版本不得覆写）；另需确认JW React SDK准确registry身份/访问权限。当前范围不授权修改或发布共享包。

### 后端及范围独立预审（非正式最终审计完成）

- 当前HEAD `4ef9b3856ee623180458855de7f23607c6ba817b` 不变。重新执行Plan §8最终受影响模块选择器：`/tmp/tq11-backend-audit.log` exit0，28 test classes、255 tests、0 failures/errors/skips；原生授权码/PKCE/USER scope、issuer/tenant、usage审计、PDP字段及Bean/架构门禁真实执行。
- 从Step1基线到HEAD source path66，全部自身授柄/鉴神；SQL/db/manifest差异为空；新dirty仅Step11四文件，不涉及其他平台/模板。三个自身POM改动仍是先前批准的Validation/Lombok/MapStruct最小编译修订，未再修改或升级。
- 按执行技能，正式最终Spec审计必须在全部Step有验证提交后进行；Step11没有GREEN/commit，因此本次只能完成独立后端/范围预审，不能宣布正式审计结束或最终PASS。

| Literal rule | Applicability | Status | Evidence | Finding/action |
| --- | --- | --- | --- | --- |
| Rule 1 | Not applicable | N/A | 本Step无Java或业务实现编辑，仅两个TS测试及现有JSON依赖解析 | 待正确认证制品与JW安装输入后重跑，不提交失败Step |
| Rule 2 | Not applicable | N/A | 本Step无Java或业务实现编辑，仅两个TS测试及现有JSON依赖解析 | 待正确认证制品与JW安装输入后重跑，不提交失败Step |
| Rule 3 | Not applicable | N/A | 本Step无Java或业务实现编辑，仅两个TS测试及现有JSON依赖解析 | 待正确认证制品与JW安装输入后重跑，不提交失败Step |
| Rule 4 | Not applicable | N/A | 本Step无Java或业务实现编辑，仅两个TS测试及现有JSON依赖解析 | 待正确认证制品与JW安装输入后重跑，不提交失败Step |
| Rule 5 | Not applicable | N/A | 本Step无Java或业务实现编辑，仅两个TS测试及现有JSON依赖解析 | 待正确认证制品与JW安装输入后重跑，不提交失败Step |
| Rule 6 | Applicable | BLOCKED | package/lock metadata真实且alias一致，但发布认证JSON/返回协议与批准源码不一致 | 待正确认证制品与JW安装输入后重跑，不提交失败Step |
| Rule 7 | Applicable | BLOCKED | package/lock metadata真实且alias一致，但发布认证JSON/返回协议与批准源码不一致 | 待正确认证制品与JW安装输入后重跑，不提交失败Step |
| Rule 9 | Not applicable | N/A | 本Step无Java或业务实现编辑，仅两个TS测试及现有JSON依赖解析 | 待正确认证制品与JW安装输入后重跑，不提交失败Step |
| Rule 10 | Not applicable | N/A | 本Step无Java或业务实现编辑，仅两个TS测试及现有JSON依赖解析 | 待正确认证制品与JW安装输入后重跑，不提交失败Step |
| Rule 11 | Applicable | PASS | 四个声明源码路径，未改架构、共享源码、其他平台或DDL | 待正确认证制品与JW安装输入后重跑，不提交失败Step |

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 复用既有npm/Vitest及JW alias，仅自身四路径，未修改共享模块 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-REUSE-001 | Applicable | PASS | 复用既有npm/Vitest及JW alias，仅自身四路径，未修改共享模块 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-DEP-001 | Applicable | BLOCKED | 可信registry bytes及签名已核验，但0.2.4 contract失败/JW依赖404 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-NAME-001 | Not applicable | N/A | 当前Step无Java/业务模型/注入/转换器或日期实现改动 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-VALID-001 | Not applicable | N/A | 当前Step无Java/业务模型/注入/转换器或日期实现改动 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-MODEL-001 | Not applicable | N/A | 当前Step无Java/业务模型/注入/转换器或日期实现改动 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-CONVERT-001 | Not applicable | N/A | 当前Step无Java/业务模型/注入/转换器或日期实现改动 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-LOG-001 | Not applicable | N/A | 当前Step无Java/业务模型/注入/转换器或日期实现改动 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-BEAN-001 | Not applicable | N/A | 当前Step无Java/业务模型/注入/转换器或日期实现改动 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-UTIL-001 | Not applicable | N/A | 当前Step无Java/业务模型/注入/转换器或日期实现改动 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-JSON-001 | Applicable | BLOCKED | 可信registry bytes及签名已核验，但0.2.4 contract失败/JW依赖404 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-TIME-001 | Not applicable | N/A | 当前Step无Java/业务模型/注入/转换器或日期实现改动 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-CONFIG-001 | Applicable | BLOCKED | 可信registry bytes及签名已核验，但0.2.4 contract失败/JW依赖404 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-PATTERN-001 | Not applicable | N/A | 当前Step无Java/业务模型/注入/转换器或日期实现改动 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-SCOPE-001 | Applicable | PASS | 复用既有npm/Vitest及JW alias，仅自身四路径，未修改共享模块 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-TEST-001 | Applicable | BLOCKED | 可信registry bytes及签名已核验，但0.2.4 contract失败/JW依赖404 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |
| MC-BLOCKER-001 | Applicable | BLOCKED | 可信registry bytes及签名已核验，但0.2.4 contract失败/JW依赖404 | Step11尚未通过 | 正确制品/registry输入后重跑；无静默例外 |

- Requirements disposition：REQ-017 `Not satisfied`（已获取并真实安装/执行的0.2.4未实现指定合同）；REQ-019 `Partial`（当前制品exit gate未通过，PG/Redis/卷/TLS/browser/负载等仍Runtime unverified）；正式全量requirement/Literal/Manual最终矩阵不得用本次预审冒充。
- Verdict仍 `BLOCKED — Step11 published artifact contract mismatch; JW existing dependency unavailable`。不启动服务/浏览器/数据库/Redis/部署，不push/PR，不发布制品。


### Step 11 organization-link follow-up（2026-10-11）

- 用户提供 `https://www.npmjs.com/settings/egon-cola/packages`；该settings管理页直接访问返回403，不能据此推断所有私有包名称或内容。
- 公共registry scope搜索（`npm search --json --searchlimit=100 scope:egon-cola --registry=https://registry.npmjs.org`）目前仅返回 `@egon-cola/admin-web-shared` 0.2.4，registry搜索时间为 `2026-09-07T01:48:22.994Z`；这与刚核验的旧认证API制品一致。搜索索引不是完整私有包清单。
- 复用本机现有npm身份做只读组织包列表 `npm access list packages egon-cola --json --registry=https://registry.npmjs.org`，exit1/E401，当前认证不可用。没有读取、输出或请求用户粘贴token，也不启动browser/login交互。
- 仍需要准确package详情链接（`https://www.npmjs.com/package/@egon-cola/...`）或修复本机npm授权后提供私有包身份；组织管理页本身不能解除Step11制品契约与SDK输入门禁。不扩大修改/发布范围。


### PC-013 — 用户确认两个实际发布名称（2026-10-11）

- 用户提供 @egon-cola/rbac3-react-sdk 与 @egon-cola/admin-web-shared 的详情链接，前者0.1.1。按最新用户AGENTS必要直接依赖授权，JW自身package.json补入本Step：逻辑tianquan-jianshen-react-sdk改用npm:@egon-cola/rbac3-react-sdk@0.1.1，不改源码import/版本/SDK源，不新增JW lock。
- Step11五源路径，全局库存71。当前baseline/既有dirty保留，所有五路径GREEN才可提交。
- 再查admin-web-shared0.2.4的integrity与此前核验完全相同；具体包名已经明确，不再请求同一信息。认证协议内容问题未被名称确认解除。


### PC-013实际安装/验证结果（2026-10-11）

- `rbac3-react-sdk@0.1.1` 的SHA512与registry一致，registry ECDSA签名通过，证据 `/tmp/tq11-artifact/sdk-trust-result.json`；installed所有published files与已验签tarball字节一致。
- JW新增必要alias后普通npm安装exit0，日志 `/tmp/tq11-jw-install-alias.log`（322 packages added，仅本host ignored node_modules；no-save/no-lock/ignore-scripts/legacy-peer-deps/no-audit/no-fund）。没有产生JW lock、源码import/版本升级或SDK源修改。原404安装阻塞已解除；不再以组织settings登录/E401作为已知公共包安装阻塞。
- 两host实际own node_modules shared均为 `@egon-cola/admin-web-shared@0.2.4`；运行时文件均与签名tarball逐字节一致。JW实际own SDK为 `@egon-cola/rbac3-react-sdk@0.1.1`，不借parent旧包或source alias。
- JW actual0.2.4 contract正常collect/import，5/5 assertion failures，exit1，日志 `/tmp/tq11-jw-024-contract.log`；普通typecheck exit2，生产LoginPage/AuthenticationShell/RoleActivation仍因密码参数/void缺remoteState失败，日志 `/tmp/tq11-jw-024-typecheck.log`。SW先前actual0.2.4同样5/5失败及typecheck exit1。
- 当前剩余阻塞是公开发布shared0.2.4的真实认证合同不符合批准源码；两个实际包名已确认、解析安装已完成，不再重复索要package链接。必须由既有共享包发布流程提供与当前OIDC导航/remoteState/CSRF实现一致的新制品（例如新的补丁版本）并更新本Step制品版本输入，才能GREEN。当前“获取安装”授权不含重建发布共享包，不改变两自身代码以迁回旧密码协议来假通过。
- Step11五路径仍未提交：两个pending contract tests、SW package.json/lock、JW package.json；严格文档validator及diff --check通过，MC-TEST/JSON/CONFIG/BLOCKER仍Blocked；正式最终审计不能越过失败Step。后端上一轮255测试证据保持，HEAD不变，无其他平台/模板/SQL/服务/部署/push/发布动作。


### PC-014 / Step11 release authorization（2026-10-11）

- 用户明确授权按现有流程构建并发布与当前认证源码一致的 @egon-cola/admin-web-shared@0.2.5、修订Spec/Plan、安装到授柄/鉴神；仍不接入其他平台/模板，不启动服务/部署。本节取代历史仅获取0.2.4、不允许发布的当前限制，历史失败证据保留。
- Baseline HEAD仍4ef9b3856，五个原Step11 dirty路径保留；新增publisher package.json/lock/README三个必要授权路径，Step11合计八路径/全局74 unique。除publisher元数据/文档与构建发布外不改shared认证源码。
- 既有README release:patch流程分阶段执行以便publish前真实验包：仅改publisher实际name/version及lock root，使用原typecheck/test/build脚本；不触发额外升级/新框架。原postbuild删除本publisher开发node_modules的行为保持。
- npm whoami 当前E401；已请求用户在本机恢复发布身份，不索取token/不启动GUI。继续准备并验证具体tarball与两个host；尚未发布不得计registry验收/完成commit。发布动作已有精确授权，身份恢复后不重复索取发布批准。
- 消费者测试原0.2.3/0.2.4各5项RED事实保留，期望版本更新0.2.5；两host先本地tarball验收再由同一tar发布、取回、签名/字节比对与registry安装，不能以源码alias代替。


### PC-014 release resumed / candidate evidence（2026-10-11）

- 用户已完成npm登录并明确要求继续；fresh `npm whoami` exit0为allendalexander。身份阻塞解除；同版本0.2.5发布前registry查询E404（未存在），不覆写既有版本。
- publisher `npm ci --legacy-peer-deps --ignore-scripts --no-audit --no-fund` exit0，仅使用既有lock；typecheck exit0、源suite14 tests/3 files passed，原 `npm run build`（prebuild favicon、tsc declarations、Vite、postbuild删除本publisher开发node_modules）exit0。无认证源码修改；声明真实login()/stepUp():void、logout Promise<GatewayLogoutResult>。
- 候选pack `@egon-cola/admin-web-shared@0.2.5`，874520 bytes，SHA512 `sha512-aDjAkdQzMH+fraQgvFfD7bNitFVpZnqbrs+OsmRb3h4YXTVvdhmySOfHjSOseKmU5pskxrtddRxeRlUI0fUqHw==`；tarball `/tmp/tq11-025/egon-cola-admin-web-shared-0.2.5.tgz`。打包不含node_modules/tests/.npmrc，源码类型及files清单已核对。
- 两host正常安装此精确候选tarball（仅预发布验证，非声称registry发布成功），各自5项实际artifact contract GREEN，两个常规typecheck均exit0。未用源码alias或mock共享模块；此前旧0.2.3/0.2.4行为RED保留。两端全suite正在验收，通过后发布这一候选tarball，registry回取核对相同字节后安装。


### Step11 prepublish gate / EOTP事实（2026-10-11）

- 所有非认证发布前检查已通过：publisher14、SW55、JW64，合计133 tests；三项typecheck exit0，two installed candidate contract各5 GREEN。
- `npm publish /tmp/tq11-025/egon-cola-admin-web-shared-0.2.5.tgz --access public --ignore-scripts --registry=https://registry.npmjs.org --auth-type=legacy` 已实际执行；exit1/EOTP。npm whoami有效不代表二次发布认证完成；未声称0.2.5已发布，未重建候选包。
- registry要求安全钥/一次性验证，已向用户提出缺失认证信息请求，不索取密码/token，不自动打开browser。发布已有授权，不重复问批准；二次验证完成后继续同一已验证tarball。
- 证据 `/tmp/tq11-025/publish.log`；candidate sha512/47files/874520bytes不变。直到registry确认发布和取回同字节之前不提交Step11作为完成。


### Step 11 final gate — Verified（2026-10-11）

- Authoritative release: `@egon-cola/admin-web-shared@0.2.5`，用户精确批准；经一次EOTP失败与用户二次验证后，同一已验收tarball发布exit0。npmjs处理完成，版本公开可读，registry time=2026-10-11T01:41:12.367Z。没有覆写0.2.4、重复成功发布或保存认证密码/token/验证码到仓库。
- registry metadata/SHA512/签名核验及全tar字节比对 PASS：874520 bytes，47 files；SHA512 `sha512-aDjAkdQzMH+fraQgvFfD7bNitFVpZnqbrs+OsmRb3h4YXTVvdhmySOfHjSOseKmU5pskxrtddRxeRlUI0fUqHw==`，SHA1 bff824fdc682159197cf5ea529d3d8c1e7e91012。registry回取与已测试候选逐字节相同；证据 `/tmp/tq11-025/registry-trust-result.json`。
- 两host已按registry精确alias安装0.2.5，各47 installed files与签名release字节一致；JW React SDK同版本0.1.1真实rbac3名称alias，own node_modules解析，不借parent旧产物。SW保留^0.2.0范围/已有lock，JWshared明确^0.2.5、SDK精确0.1.1且不新建lock，所有源码logical imports保持。
- 原0.2.3/0.2.4实际artifact各5行为RED保留；新artifact两host各5 contract GREEN。GREEN后移除导航case中的旧async rejection兼容catch，避免遮蔽真实异常；最新registry installed contract各5再次PASS，两个registry安装后的typecheck exit0。
- publisher typecheck/test14/build/pack exit0；SW fullsuite55、JW fullsuite64（均包含新5项），合计133前端相关tests。正式包与这些suite用的候选完全相同。固定jsdom getComputedStyle pseudo-element提示为测试环境警告，0 failed/skip；不算浏览器证据。
- Plan §8 exact受影响Maven gate `/tmp/tq11-final-backend.log` exit0：28 classes、255 tests、0 failure/error/skip。
- Source diff审阅：八声明路径；publisher lock全部依赖节点不变，仅top/rootname/version；SW除root shared依赖与shared节点外其他lock节点不变；JW无checked-in lock。無依赖框架/其它依赖版本声明升级，無Java/SQL/其它platform/template变更。strict Plan validator、diff --check PASS。
- 发布已执行且不可用本地源码回滚代替registry撤销；若后续发现缺陷用新补丁forward fix，不覆写0.2.5。现有服务未启动/部署；第三方真实SSO/PG/Redis/卷/TLS/browser/负载继续Runtime unverified。

| Literal rule | Applicability | Status | Diff evidence | Validation evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Not applicable | N/A | 没有新Java/业务数据类型；两个TS contract tests与6个元数据/文档文件 | git八路径 | No unresolved Step finding | None |
| Rule 2 | Applicable | PASS | 验证已批准无凭据OIDC导航、CSRF挑战后logout、503远端状态及非法CSRF拒绝；无新Java交接 | 两个真实installed5项+publisher源14 | No unresolved Step finding | None |
| Rule 3 | Not applicable | N/A | 没有新Java POJO/VO或Converter；认证源保持 | 源码diff仅元数据/文档与测试 | No unresolved Step finding | None |
| Rule 4 | Not applicable | N/A | 无Java/Spring业务Bean/注入改动 | 八路径均TS/JSON/Markdown | No unresolved Step finding | None |
| Rule 5 | Applicable | PASS | 复用已有npm/Vite/tsc/Vitest及JDK/Node标准crypto校验，无新工具库/框架 | lock依赖节点不变 | No unresolved Step finding | None |
| Rule 6 | Applicable | PASS | metadata JSON实际匹配registry身份/版本/hash；logout JSON契约三状态与CSRF错误实际验证 | 签名+字节+真实HTTP mock边界 | No unresolved Step finding | None |
| Rule 7 | Applicable | PASS | publisher root/lock name/version一致，SW alias/lock SSRI一致；JW保留无lock惯例，固定SDK版本 | 两host通常typecheck和全suite | No unresolved Step finding | None |
| Rule 9 | Not applicable | N/A | 未实现新Complex业务流；既有release工具分阶段验证同一artifact | 没有新增业务实现/调度抽象 | No unresolved Step finding | None |
| Rule 10 | Not applicable | N/A | 没有Java日期实体/日期实现改动；仅记录registry UTC发布证据 | git diff | No unresolved Step finding | None |
| Rule 11 | Applicable | PASS | 既有publisher及两自身host；无其他平台/模板/DDL/架构移动 | 八路径范围/255架构及回归 | No unresolved Step finding | None |

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 原模块和import边界保持; Rule 11 evidence | No unresolved Step finding | None |
| MC-REUSE-001 | Applicable | PASS | 原build/prebuild/postbuild、npm alias与registry工具; Rule 5 evidence | No unresolved Step finding | None |
| MC-DEP-001 | Applicable | PASS | 仅明确授权版本/identity，非shared节点未变; Rule 5 evidence | No unresolved Step finding | None |
| MC-NAME-001 | Not applicable | N/A | 本Step无Java类型; Rule 1 evidence | No unresolved Step finding | None |
| MC-VALID-001 | Applicable | PASS | 真实CSRF/logout/无密码导航负例; Rule 2 evidence | No unresolved Step finding | None |
| MC-MODEL-001 | Not applicable | N/A | 无模型修改; Rule 3 evidence | No unresolved Step finding | None |
| MC-CONVERT-001 | Not applicable | N/A | 无Converter修改; Rule 3 evidence | No unresolved Step finding | None |
| MC-LOG-001 | Not applicable | N/A | 无业务日志修改，未保存认证secret; Rule 4 evidence | No unresolved Step finding | None |
| MC-BEAN-001 | Not applicable | N/A | 无Bean修改; Rule 4 evidence | No unresolved Step finding | None |
| MC-UTIL-001 | Applicable | PASS | 仅既有工具与标准crypto，未加repo helpers; Rule 5 evidence | No unresolved Step finding | None |
| MC-JSON-001 | Applicable | PASS | 实际三状态JSON与manifest/lock一致; Rule 6 evidence | No unresolved Step finding | None |
| MC-TIME-001 | Not applicable | N/A | 无日期实现修改; Rule 10 evidence | No unresolved Step finding | None |
| MC-CONFIG-001 | Applicable | PASS | 元数据一致/两host各自惯例; Rule 7 evidence | No unresolved Step finding | None |
| MC-PATTERN-001 | Not applicable | N/A | 业务源码保持，机械release流程; Rule 9 evidence | No unresolved Step finding | None |
| MC-SCOPE-001 | Applicable | PASS | 八声明路径，publisher特例已有直接批准; Rule 11 evidence | No unresolved Step finding | None |
| MC-TEST-001 | Applicable | PASS | 133前端相关+255后端+两端registry5/5，three typecheck exit0; Rule 2 evidence | No unresolved Step finding | None |
| MC-BLOCKER-001 | Applicable | PASS | 签名/字节/实际安装/所有Step门禁已闭合；live验收明确另列; Rule 11 evidence | No unresolved Step finding | None |

### Step 11 — Committed

- Commit `920e7099be6d0411cc58639dbede5d278ec04c7e`；exact8 paths verified. Registry0.2.5发布与两端安装/完整门禁通过；此HEAD为正式最终审计冻结基线。
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/src/auth/AuthArtifactContract.test.ts`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/src/features/auth/AuthArtifactContract.test.ts`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/package-lock.json`
- `egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web/package.json`
- `egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web/package.json`
- `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/package.json`
- `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/package-lock.json`
- `egon-cola-xingyuan/egon-cola-xingyuan-admin-web-shared/README.md`


## 13. Final Spec conformance audit — 2026-10-11

本节为正式独立审计，替代历史Blocked/预审的当前交付状态；历史记录保留为执行证据。全部11 Steps及一个明确归属的fixture corrective commit已验证提交。Plan `Completed`仅表示批准的实施与本次审计完成，不代表实际部署/生产Ready。

- Frozen source baseline: `main@920e7099be6d0411cc58639dbede5d278ec04c7e`；scope基线为 `214e53fd1^`（原始Plan baseline 3a82ea276）。当前源码clean，任务Spec/Plan文档在本次审计后以独立文档提交保存。

| Effective Spec | Effective revision / applicability |
| --- | --- |
| 2026-10-10-12-36-tianquan-security-self-hardening.md | Accepted；本次PC-005–014与用户依赖/字段闸门/RPC/发布授权；全部本轮有效要求 |
| 2026-09-22-17-14-tianquan-multitenant-oauth-oidc-design.md | Accepted，Updated2026-09-23 10:51；主Spec列出的§7/9/15.1/15.2/16等保留合同 |
| 2026-10-08-10-18-oauth-current-code-alignment.md | Review，Updated2026-10-09 17:37；仅主Spec明确采纳的§6/9.4/9.5/11/15，不把整份Review扩为实施范围 |
| 2026-08-17-09-37-rbac3-annotation-userdetails-field-authorization.md | Accepted，Updated2026-08-17 15:07；原读/方法PEP合同与本轮显式WRITE闸门对§3.2的批准修订 |
| 2026-09-24-12-00-outbox-statemachine-cqe-integration.md | Implemented，Updated2026-09-27 08:52；仅原CQE/同源事务/技术投递边界，未改组件 |

### 13.1 Delivery and fresh final validation

各Step精确paths/逐项Gate/RED-GREEN在前文Commit记录中；下表hash已与git实际历史/路径核对，不替代行为证据。

| Step | Status | Commit | Exact path/evidence anchor |
| --- | --- | --- | --- |
| 1 | Committed | 214e53fd1 | 前文Step1 exact7/CORS/OpenAPI记录 |
| 2 | Committed | 26f77b7c2 | 前文Step2 exact10/字段WRITE/真实PDP/编译补齐 |
| 3 | Committed | cb7539696 | 前文Step3 exact2/strict双介质 |
| 4 | Committed | dcf282aa7 | 前文Step4 exact8/receipt、DB提交顺序、有界replay |
| 5 | Committed | 8a4a30c82 | 前文Step5 exact8/真实JWS/cache/acquire |
| 6 | Committed | a415c8aef | 前文Step6 exact15/proof/HTTP/RPC |
| 7 | Committed | 5ecc8228b | 前文Step7 exact10/host registry及delivery指标 |
| 1 corrective | Committed | 8ae4ac3a0 | 仅原OAuthTokenClientCredentialsTest ValidationAutoConfiguration fixture，一项既有路径，非生产行为 |
| 8 | Committed | 1e311c289 | 前文Step8 exact4/native UserInfo一次验签及事件 |
| 9 | Committed | 62ad8498a | 前文Step9 exact8/PDP Decorator/错误族/去重 |
| 10 | Committed | 4ef9b3856 | 前文Step10 exact12/6profiles/TLS/readiness及PC011直接guard消费 |
| 11 | Committed | 920e7099b | 前文Step11 exact8/真实0.2.5 registry发布、alias、两host installed contract |

| Final command/gate | Actual final result | Evidence |
| --- | --- | --- |
| Plan §8 exact Maven affected-module selector（离线、不启integration profile） | exit0；28 classes、255 tests、0 failure/error/skip | /tmp/tq-final-audit-backend.log；冻结source之后正式重跑 |
| npm --prefix SW run test | exit0；13 files、55 tests | /tmp/tq-final-audit-sw.log；冻结source之后正式重跑 |
| npm --prefix JW run test | exit0；20 files、64 tests | /tmp/tq-final-audit-jw.log；冻结source之后正式重跑 |
| npm --prefix SW/JW run typecheck | 各exit0；registry0.2.5 installed真实解析 | /tmp/tq11-sw-registry-typecheck.log、/tmp/tq11-jw-registry-typecheck.log；当前源码字节未改 |
| publisher原typecheck/test/build/pack | exit0；源14 tests、47文件candidate，未改认证源 | /tmp/tq11-publisher-*.log、/tmp/tq11-025/pack.json；构建脚本原行为，包括postbuild清开发node_modules |
| registry签名/完整性/字节/installed核验 | PASS；0.2.5 tar874520bytes，候选与registry相同，两host各47files相同；SDK0.1.1已核验 | /tmp/tq11-025/registry-trust-result.json、/tmp/tq11-artifact/sdk-trust-result.json |
| npmjs公开release | 已发布@egon-cola/admin-web-shared@0.2.5；registry time2026-10-11T01:41:12.367Z；精确用户授权 | https://www.npmjs.com/package/@egon-cola/admin-web-shared/v/0.2.5；publish-confirmed.log；初次EOTP失败未假报成功 |
| strict Plan validator / diff --check | PASS | 真实命令exit0；全部74 source paths与§5 inventory相等，无undeclared path |
| source新增危险模式检查 | 0 @Autowired、0 Gson/Fastjson、0 Date/Calendar imports、0 BeanUtils.copyProperties、0 ordinal业务编码 | git diff baseline..frozen逐新增行检查；仅补充人工审查 |
| immutable SQL/DDL/其他platform/template边界 | 74源码路径均两个自身模块+明确授权publisher3元数据/文档；0 SQL/db/manifest或他平台/模板diff | git diff 214e53fd1^ frozen --name-only；没有迁移/generator动作 |

jsdom `getComputedStyle` pseudo-element提示与原deprecated/unchecked编译提示是测试/既有编译环境警告，未造成失败/跳过。没有把H2、MockMvc、Redisson double或JSdom结果当作生产PG/Redis/browser证明。

### 13.2 Requirement conformance matrix

主Spec§4每个REQ独立评价。Satisfied仅代表相应实现及本轮获准非runtime证明完整；与这些要求关联的实际运行条款在后续RUNTIME-*单独列出，不通过源检查覆盖。

| Requirement | Effective source | Expected behavior | Commit evidence | Code/symbol evidence | Fresh validation evidence | Status | Gap/impact/action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-001 | 主Spec§4/相关§7–16及PC014 | 仅自身/批准publisher，其他接入延期 | all Steps | 74-path inventory / eight-step11 scope | 范围与SQL差异检查 | Satisfied | None |
| REQ-002 | 主Spec§4/相关§7–16及PC014 | 保留原协议/PKCE/redirect/密码/claims/C10/tenant/方法PEP | 1,2,5,6,8,9,10,11 | 原SAS/IdpJwtVerifier/DefaultAuthorizationService边界保持 | native/OIDC/consent/persistence/client/resource-grant/issuer/field/method regression in255 | Satisfied | Live原协议验证见RUNTIME-005/006 |
| REQ-003 | 主Spec§4/相关§7–16及PC014 | 四原生资源精确CORS，其余cookie/管理路径不开放 | 214e53fd1 | OAuthCorsProperties / OAuthNativeCorsProcessor / own chains | OAuthNativeCorsContractTest5 + OAS2 | Satisfied | None |
| REQ-004 | 主Spec§4/相关§7–16及PC014 | 原错误族401/403/503、no-store、pre-render审计 | a415c8aef,1e311c289,62ad8498a | Idp/Rbac filters、nativeFailureHandler、Rbac3AuthorizationExceptionHandler | real chain5 + observer14 + native metrics cases | Satisfied | None |
| REQ-005 | 主Spec§4/相关§7–16及PC014 | 可信Token使用事实、拒绝独立事实、匿名不伪claims、public/preflight排除 | a415c8aef,1e311c289,62ad8498a | ObservedVerification、SecurityObservationFilter、native provider、RPC proof、PDP observer | observer14/RPC5/native15/PDP真实五方法及去重 | Satisfied | 实际全链路使用与消息见RUNTIME-001/002/006 |
| REQ-006 | 主Spec§4/相关§7–16及PC014 | force+atomic/directory durability+confirmed MQ后才放行 | cb7539696,dcf282aa7,a415c8aef | TokenUsageAuditComponent.appendStrict / native provider / observed verifier | producer10、strict失败零业务case | Satisfied | 真实卷掉电和broker故障见RUNTIME-002/003 |
| REQ-007 | 主Spec§4/相关§7–16及PC014 | DB save commit返回→signed receipt→XACK，sender仅验真后清本地 | dcf282aa7 | OAuthAuditConsumer.publishReceipt / producer.acknowledge / ReceiptDTO | consumer15/receipt10/producer10 | Satisfied | 真实PG事务/Redis Lua/ACK需RUNTIME-001/002 |
| REQ-008 | 主Spec§4/相关§7–16及PC014 | sameid replay/指纹冲突、过期不重建、DEAD保留、有界恢复 | dcf282aa7,5ecc8228b | replayPending rotatingcursor/batch、expired consume、DEAD marker | controlled duplicate/expiry/replay/dead/capacity tests | Satisfied | 崩溃/重启/多节点实际恢复见RUNTIME-003/004 |
| REQ-009 | 主Spec§4/相关§7–16及PC014 | 真实JWS.verify成功失败计时，不计Redis/audit/业务 | 8a4a30c82,1e311c289 | Nimbus verifierFactory/Verifier Decorator in manager/native processor | MultiIssuer14 + real RSA/延迟Redis/invalid signature cases | Satisfied | QPS/p95实际性能见RUNTIME-007 |
| REQ-010 | 主Spec§4/相关§7–16及PC014 | 真实JWK/token cache HIT/MISS/EXPIRED与单飞acquire | 8a4a30c82 | BoundedJwkSetCache / IdpServiceOAuth2Client实际lookup/authorize | IdpServiceOAuth2Client16/MultiIssuer14 | Satisfied | 实际provider/cache拓扑与负载见RUNTIME-007 |
| REQ-011 | 主Spec§4/相关§7–16及PC014 | 认证和PDP观察分开、固定标签、metric故障不改变权限 | 5ecc8228b,62ad8498a | Core sample constraints / twohost strategy recorders / Rbac3ObservedAuthorizationService | both metrics15/15、真实Default PDP/exception instance preservation | Satisfied | exporter/dashboard/告警运行见RUNTIME-009 |
| REQ-012 | 主Spec§4/相关§7–16及PC014 | 当前USER显式overall ALLOW且每项WRITE，一次原PDP | 26f77b7c2 | FieldWriteAuthorizationCommand.WriteFields / component / generated converter | FieldWriteAuthorizationComponentTest10 + DefaultPDP/method/field regression | Satisfied | 业务接口接入明确Non-goal，不计未实现 |
| REQ-013 | 主Spec§4/相关§7–16及PC014 | server提交presence含null、不从UI/DTO值推权限、不自动过滤 | 26f77b7c2 | server-owned command无tenant/subject/role，defensive fields snapshot | missing/read/masked/none/null-presence/无USER/SERVICE负例 | Satisfied | None |
| REQ-014 | 主Spec§4/相关§7–16及PC014 | 全层约束、模型/MapStruct/Bean/JSON/java.time/组件规范 | 2,4,5,6,7,8,9,10 | Jakarta groups/constraints、BaseForward mappers、qualified Lombok constructors、Core port | 255 tests + generated mapper/constructor/config schema + final10Rules/17MC人工审查 | Satisfied | 未迁移历史JA JPA/Flyway，明确原scope保持 |
| REQ-015 | 主Spec§4/相关§7–16及PC014 | 六profiles核心键同构，self opt-in/default-off、缺依赖拒绝 | 4ef9b3856 | usage/delivery properties、twohost configuration / disabled SDK path | realYaml loader/Binder/Validator + contextRunner missing/primary/lease guards | Satisfied | 部署值需RUNTIME-004/005 |
| REQ-016 | 主Spec§4/相关§7–16及PC014 | HTTPS/TLS可信边界、原Cookie CSRF和代理信任不弱化 | 214e53fd1,4ef9b3856 | secure transport guards/prod TLS defaults/local restricted exception/原trust guards | TLS/config negative cases与native CSRF regression | Runtime unverified | 源守卫已证；真实证书/入口/代理/内部TLS未启动验收，RUNTIME-005 |
| REQ-017 | 主Spec§4/相关§7–16及PC014 | PC014精确0.2.5真实制品、两host普通typecheck/fullsuite | 920e7099b | signed registry0.2.5、fixed npm aliases/lock、actual twohost contract suites | 两host55/64 + registry各5 + typecheck0 + allfiles byte equal | Satisfied | 浏览器与真实Edge联动仍RUNTIME-006 |
| REQ-018 | 主Spec§4/相关§7–16及PC014 | 历史SQL/DDL/manifest/tenant事务不改，无migration/generator | all Steps | unchanged original managedMP/Outbox/JPA SQL与schema | 全74路径无.sql/db/manifest变更 | Satisfied | 本轮新业务UK/DDL条款N/A，无迁移宣称 |
| REQ-019 | 主Spec§4/相关§7–16及PC014 | 可接入source/controlled/live门禁分开，不称生产Ready | all Steps | §16 SELF-SOURCE/SELF-CONTROLLED/SELF-RUNTIME gates + this audit | source与controlled通过；无service/DB/browser/load执行 | Runtime unverified | 实际self部署门禁未验收，后续其他平台/模板仍不得计完成 |


### 13.3 Detailed clauses, test obligations, and runtime gaps

| Clause | Evidence / disposition | Status | Action |
| --- | --- | --- | --- |
| TEST-001–004 CORS/OAS/原CSRF保持 | 实际native CORS5/OAS2/consent25/old公开OAS1 | Satisfied | None |
| TEST-005–010 JWT allow/failure/audit strict HTTP/RPC/native | observer14、JWT14、RPC5、host真实链、native真实SAS场景；验证业务零调用 | Satisfied | None |
| TEST-011–016 receipts/failure/forgery/replay/expiry/DEAD | consumer15/receipt10/producer10与host容量/lease case；真实HMAC/临时FS、受控broker/DB | Satisfied | 真PG/Redis/掉电另列，未假报integration |
| TEST-017–021 explicit WRITE and existing read/method | field write10、defaultPDP3、serializer4、method3，真实生成Mapper/Spring validation | Satisfied | None |
| TEST-022–024 real crypto/cache/categorized metrics | 真RS256、真实cache lookup、manager.authorize、real SimpleMeterRegistry/DefaultPDP | Satisfied | None |
| TEST-025 retained native/C10/tenant boundaries | issuer11、consent25、OIDC17、persistence16、client4、grant6、Core policy3等 | Satisfied | None |
| TEST-026 configuration/Bean/validation | metrics各15含6Yaml/Binder/negative TLS；constructor8；autoCfg8；Admin架构8 | Satisfied | None |
| TEST-027 artifact ordinary tools | 签名registry0.2.5、全tar/installed47files比对、twohost各5与full55/64、typecheck0 | Satisfied | None |
| TEST-028 authorized non-runtime scope/SQL/exit gate | 全路径及历史SQL不可变证明，真正runtime层按下列缺口分开 | Satisfied | 不把此行视为live gate通过 |
| API_OPTIONS/HTTP/internal/Event compatibility | 原业务HTTP/routes/成功wrapper不变；TokenUse schema1/receipt schema1；旧constructors/SPIs保留；真实userinfo单次verify | Satisfied | None |
| Rollout/rollback boundary | consumer先兼容receipt、producer self开启；未确认spool不删，已发布0.2.5用forward补丁修复不覆写；未实际部署 | Satisfied | 实际cutover见runtime；发布不可当local源码回滚 |
| Persistence/uniqueness/CQE boundary | 未新增关系模型/PO/UK/SQL；保留原MP/Outbox同源transaction，usage走真实Streams append，非本地event仅模拟可靠投递 | Satisfied | 新DB唯一键/DDL实施N/A；部署事务需RUNTIME-001 |
| Scope/non-goals | 无其他platform/template/Portal/server/gateway改动；仅批准publisher3元数据/文档；字段业务接口、通用DTO过滤与JPA迁移未扩入 | Satisfied | 后续接入另授权 |
| RUNTIME-001 actual PostgreSQL commit/unknown/duplicate/180d purge | 源save事务边界与受控顺序已证；没有跑真实PG | Runtime unverified | 用户启动隔离PG后验证真实commit→receipt与重送幂等 |
| RUNTIME-002 Redis PRIMARY Lua/receipt TTL/ACK/failover/ACL | 源atomic Lua/fingerprint/HMAC与mock路径有证据，没连真实broker | Runtime unverified | 实际PRIMARY/producer-consumer ACL、ACK/故障切换验证 |
| RUNTIME-003 persistent volume force/atomicMove/directory durability/power loss | JDK真实临时FS/negative branch已证，不等于用户实际持久卷掉电保证 | Runtime unverified | 目标卷能力、掉电/restart/SENT timeout恢复测试 |
| RUNTIME-004 multi-node topology/spool exclusivity/HMAC mounts | 本地双context lease拒绝/释放已证，未跑多pod/multi-node | Runtime unverified | 各node独立持久卷与同key/跨节点consumer-confirm验证 |
| RUNTIME-005 deployment HTTPS/TLS/proxy/cookies | 原强信任与配置拒绝已证，没有加载部署证书或启动服务 | Runtime unverified | 验证实际证书、public/issuer/Redis/RPC/Tianshu TLS与可信代理 |
| RUNTIME-006 self real browser/Edge/OIDC/step-up/logout revocation | actual SDK契约及SAS controlled chain已证，未开browser/真实IdP | Runtime unverified | 用户启动后测试真实SSO/CSRF/撤销/页面与服务细权联动 |
| RUNTIME-007 QPS/request p95/crypto/cache/spool capacity | 计时点/计数真实，未做负载测量；不以crypto timing承诺全请求5ms | Runtime unverified | 目标QPS和durable audit成本、batch/TTL/cache命中率压力测试 |
| RUNTIME-008 actual JWK rotation/revocation projection restart | 原周期/世代/strict revocation机制与源码/native tests保留；未跑部署重启 | Runtime unverified | 验证active/overlap keys、共享撤销、投影恢复和过期token |
| RUNTIME-009 exporter/dashboards/alerts | host MeterRegistry/低基数gauge/timer/counter有证，未部署监控告警 | Runtime unverified | 配置并测试spool80%/full/DEAD、receipt age、认证异常与exporter告警 |

没有发现本轮范围内尚未实现的代码要求或需要新的未批准架构/库/DDL修复。上述Runtime unverified是明确未获启动授权的运行证据，不是假借runtime掩盖源缺陷，也不是允许立即接入其他平台的结论。

### 13.4 Independent final Literal Rule gate

本表依据冻结tree/全部delivery diff重新逐项检查，不直接复制Step predicted statuses；命名、模型与业务/技术factory分类以当前声明为准。

| Literal rule | Applicability | Status | Final diff/path/symbol evidence | Final validation evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | Behavior Component/Filter/Processor/Configuration/Service、Command/BO/ReceiptDTO语义清晰；无ambiguous新carrier | 74-path type inventory + full compile | No source-conformance finding | None；对应live evidence另列 |
| Rule 2 | Applicable | PASS | WRITE groups/native constraints/cascade；sample AssertTrue shape；proof/Context/Event/receipt schema；PDP入/出及非proxy显式Validator；旧DAO层无新边界 | field10/observer14/receipt10/config15x2/real method-validation negative | No source-conformance finding | None；对应live evidence另列 |
| Rule 3 | Applicable | PASS | Command/Context/Metric/Receipt普通class完整@Data@NoArgs@AllArgs@Accessors@Builder；Proof及FieldPolicyDecision immutable record防御复制；两个Mapper extends BaseForwardConverter | 生成Mapper实际执行、compiler与构造测试；无BeanUtils/JSON roundtrip | No source-conformance finding | None；对应live evidence另列 |
| Rule 4 | Applicable | PASS | 新业务components @Slf4j/RequiredArgsConstructor final qualified fields；两host命名factory、primary Supplier；根lombok qualifier复制；只保留旧兼容技术构造adapter | SpringConstructorResolution8/actualStarter override/contextRunner/源码注入逐类审查 | No source-conformance finding | None；对应live evidence另列 |
| Rule 5 | Applicable | PASS | JDK/既有Spring/Nimbus/Egon utils；未新增Utils或第三方运行库；publisher原工具链 | 新增行imports/3批准POM与lock-node不变检查 | No source-conformance finding | None；对应live evidence另列 |
| Rule 6 | Applicable | PASS | Boot Jackson唯一Java JSON；proof JsonIgnore、VerifiedJwt Context ToString.Exclude；receipt strict6fields；CoreTransient隐藏schema辅助getter；原enum编码未变 | receipt strictschema/JSON4/Valid2fields、无Gson/Fastjson、npmJSON/actual wire tests | No source-conformance finding | None；对应live evidence另列 |
| Rule 7 | Applicable | PASS | 6profiles核心usage/delivery/RPC/Tianshu TLS叶子同构，test/migration关闭；现有env值独立；publisher/alias/lock元数据一致 | 实际YamlPropertySourceLoader/Binder/Validator与negative TLS；两host正式安装 | No source-conformance finding | None；对应live evidence另列 |
| Rule 9 | Applicable | PASS | 实际Decorator（JWS/PDP/verification）、bounded method-ref Strategy（9meters）、Chain callback/native CORS与原technical spool State生命周期；WRITE小guard保持直接且委托原PDP | 真实RS256/cache/PDP/strict/missing metrics failure branches | No source-conformance finding | None；对应live evidence另列 |
| Rule 10 | Applicable | PASS | Instant/Duration/Clock保持UTC/注入；cache TTL沿原nanoTime单调计时不假日期；无新增util Date/Calendar/SDF模型 | 新增imports0 + Clock/expiry/replay/receipt/time constraint tests | No source-conformance finding | None；对应live evidence另列 |
| Rule 11 | Applicable | PASS | 原传统Admin/Starter边界、Core port依赖方向；MP/Outbox reuse与usage真实MQ；零schema/模板/其他平台变更，仅publisher用户明确例外 | AdminLayerBoundary8 + 74-source inventory/diff/CQE顺序源审阅 | No source-conformance finding | None；对应live evidence另列 |


### 13.5 Independent final Manual Check gate

| Check ID | Applicability | Status | Final evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 实际传统分包/技术Starter边界不变，AdminLayerBoundary8；usage Redis Streams MQ而非local event，Outbox同源原实现保留 | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-REUSE-001 | Applicable | PASS | 原Spring Security/SAS/Nimbus/Boot validation与唯一registry；Egon ValidationUtils/BaseForward、原MP/Outbox/SPI/client manager；无第二auth/metric框架 | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-DEP-001 | Applicable | PASS | 三个自身POM仅先前明确批准编译API/provided/processor；两个现有SDK logical keys复用alias，精确user授权0.2.5发布；publisher全部非root lock节点不变 | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-NAME-001 | Applicable | PASS | 新增载体Command/BO/DTO及行为角色名称逐声明复核，未为简短模型滥造record | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-VALID-001 | Applicable | PASS | FieldWrite分组、nested field access、sample跨字段AssertTrue、proof/frame/receipt/callback再入验证及原DefaultPDP；255真实负例和source review | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-MODEL-001 | Applicable | PASS | 完整普通class Lombok，immutable proof/decision/refs record；防御拷贝与schema原字段保持；没有新增PO/关系表/软删UK，此DB子项N/A且全SQL字节不变 | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-CONVERT-001 | Applicable | PASS | FieldWrite和TokenUsage两Mapper均BaseForward；ignoreByDefault显式映射/Context/proof，生成mapper在实际组件/native场景运行 | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-LOG-001 | Applicable | PASS | 具体业务class Slf4j、有限failureType单次记录；eventId安全opaque，TokenAuditContext verifiedJwt不序列化/不toString，raw token/密码/OTP/key未进仓库或metrics | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-BEAN-001 | Applicable | PASS | 具名Beans/qualified Lombok/deps及copyableAnnotations；config技术值URI不冒充依赖Bean，旧public适配构造保留，真实primary/override/disabled等测试通过 | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-UTIL-001 | Applicable | PASS | 只用JDK及既有成熟Spring/Nimbus/Egon utilities；0新增Utils/BeanUtils/Gson/Fastjson/业务反射转换，临时Node标准crypto仅验发行包 | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-JSON-001 | Applicable | PASS | Boot Jackson/严格codec与11-field usage、6-field receipt/原Valid JSON兼容；npm元数据与真实CSRF/logoutState三状态测试 | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-TIME-001 | Applicable | PASS | java.time业务时间，Clock固定测试/原单调cacheTTL；Date/Calendar新imports0；registry发布时间为UTC证据非业务日期新模型 | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-CONFIG-001 | Applicable | PASS | 6个自身YAML实际core-leaf/Binder/Validation测试，prod secure flags与三重local例外；消费者/publisheralias/lock可信meta，非root依赖不升级 | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-PATTERN-001 | Applicable | PASS | 源码真实Decorator/Strategy/Chain/State参与者、委托与错误/恢复路径；没有为WRITE简单条件添加无意义抽象，复杂流未用字符串/反射任意dispatch | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-SCOPE-001 | Applicable | PASS | 所有74source paths等于approved inventory；其他platform/template/SQL无diff；单一publisher发布只有user明确3元数据/文档例外 | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-TEST-001 | Applicable | PASS | 冻结source后fresh Maven255 + SW55/JW64，registry installed twohost typecheck0、publisher14/build + signature/allfiles证明；unit/controlled/live明确分离 | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |
| MC-BLOCKER-001 | Applicable | PASS | 11Step commits/gates与明确corrective均闭合；所有final MC/Rules source PASS；runtime缺口9项全部公开，最终verdict PARTIAL不假称生产Ready | No unresolved source check finding | None；Runtime-*需用户后续运行授权/验收 |


### 13.6 Scratch disposition, preserved work and final verdict

- 之前Step临时核验脚本按其记录删除/由模块JUnit测试承接；本轮新增orchestration Python脚本（/tmp/tq11-plan-resume.py、/tmp/tq11-publish-plan.py、/tmp/tq11-verified-commit.py及本audit写入helper）执行后删除。所有长期行为验收已落到模块JUnit/Vitest套件；没有在repo scripts/work留下本任务未归属脚本。
- /tmp/tq11-artifact与/tmp/tq11-025保留为registry原始metadata、已签名旧/新制品、publish/validation log证据；仅外部scratch不提交，不作为源别名。临时inline Node/JavaScript核验只用标准crypto/tar比对，没有额外repo测试工具遗留。
- 未改/清理其他用户文件或既有untracked脚本、未重写Git历史；源码工作树clean，最终仅本任务Spec/Plan两文档待独立路径限定文档commit。编译dist/target/node_modules/tsbuildinfo/favicon输出为原ignored构建产物，不提交。
- 已执行的外部不可逆动作只有用户明确授权的npmjs 0.2.5发布；没有启动项目/服务/数据库/Redis/browser、没有执行实际migration/部署/push/PR。npm二次认证由用户完成/提供，密码/token/验证码未写入交付文件。
- **Final verdict: PARTIAL — Spec requirements are unmet or unverified.** 本轮实现、11Step与正式审计已完成，所有source/controlled门禁通过；REQ-016/019及RUNTIME-001–009真实部署证据尚未验证。不得把该PARTIAL转述为生产Ready或其他平台/模板已接入。


### Final documentation whitespace corrective gate

文档提交 `c88d57be5` 的首次cached diff检查实际发现主Spec1550–1554五行尾部空格；该shell序列未在检查失败后停止，因而仍完成首次文档提交。明确记录此执行偏差，不将该检查假报成功、不改写历史。随后独立机械修正这五处尾空格，以路径限定格式纠正提交关闭文档gate；只重新运行strict Plan validator/diff checks，未修改任何已冻结源码、协议、验收条款或发布产物，不重复运行无关测试。最终源码基线仍920e7099b，Step11及PARTIAL结论保持，文档/源码最终worktree须clean。
