# 统一身份与 Yuheng MCP 本地联调 Runbook

本流程使用授柄的正式 PLATFORM/tenant issuer、鉴神权限、天枢HMAC/registry和独立的 Yuheng API/MCP Engine。浏览器经独立 Edge OIDC/BFF 会话登录；MCP/业务服务仍验证资源绑定的 Bearer，不读取浏览器 AT/RT cookie。运行命令只由操作者执行。

## 前置准备

先按[本机身份联调手册](../runbooks/unified-identity-local.md)和[受控切流手册](../runbooks/unified-identity-oauth-client-tenant-cutover.md)准备当前 MP identity、issuer/key/client/Resource/Grant 与真实鉴神 tenant-authority 状态。

- PostgreSQL/Redis 已由操作者运行，明确提供私密凭据文件。
- `UNIFIED_IDENTITY_OAUTH_REGISTRY_FILE` 是mode400/600的当前用户私密配置，精确格式见修订Spec§16.1。包含8个既有本地 SERVICE client 的独立 PLATFORM/tenant slots、credentialVersion、one-time secret 文件引用和两个正式 USER 输入。
- tenant-b USER 输入已有 MCP Provider Resource/Scope/角色授权；工具不会在失败时创建空scope USER Grant或扩大权限。
- `YUHENG_EDGE_BASE_URL` 指向操作者配置并运行的 Edge；默认OAUTH根路径不再提供旧密码、RT或JWK协议。
- Java21、Maven Wrapper、Node/npm和常用本地CLI工具可用。当前MCP夹具不要求启动Kafka；脚本不启动PG/Redis/Edge/Docker。

## 拓扑与端口

| 组件 | 默认端口 | 角色 |
| --- | --- | --- |
| 授柄/鉴神/玉衡Admin/天枢Admin | 18120/18130/18140/18150 | 身份、权限、发布、注册/配置 |
| 玉衡 API Engine | 18180，管理18182 | HTTP/RPC数据面 |
| 玉衡 MCP Engine A | 18185，管理18186 | 独立MCP数据面 |
| MCP Engine B | 18184，管理18183 | 独立MCP节点；保留旧 `yuheng-biz-gateway-b` PID/env兼容别名 |
| MCP Provider/Remote MCP/Mock | 18161/18151/18160 | 本地Operation、联邦、HTTP夹具 |
| Portal/四Admin Web | 18125/18121/18131/18141/18152 | 同一Edge BFF登录入口 |
| Edge | 操作者显式配置 | OAuth2 Client/TokenRelay/服务端会话 |

MCP两节点使用同一角色的执行JAR、数据库与Redis会话/任务基础设施，但节点/instance ID、数据目录和监听器独立。B不能运行API artifact，也不能占用A的18185数据端口。

## 用户运行流程

以下命令会构建或启动设施、改变联调状态；编码任务没有执行它们：

```bash
export UNIFIED_IDENTITY_OAUTH_REGISTRY_FILE=/absolute/private/local-oauth-registry.json
export UNIFIED_IDENTITY_POSTGRES_PASSWORD_FILE=/absolute/private/postgres.password
export UNIFIED_IDENTITY_REDIS_PASSWORD_FILE=/absolute/private/redis.password
export YUHENG_EDGE_BASE_URL=http://127.0.0.1:<operator-edge-port>
scripts/unified-xingyuan/start-local-stack.sh
```

启动前核查当前 MP facts 和私密材料；AS先以已配置的issuer/key/client启动，其余依赖随后启动。前端只生成 `VITE_YUHENG_ORIGIN`，API代理保持Engine，认证/回调代理指向Edge。Source/target导入是独立的人工受控CLI，启动脚本不会自动执行。

`prepare-local-stack.sh` 包含用户运行的完整编排和停止阶段，不是静态测试。只有准备完成、配置和材料经过核实时才可运行。每个JAR必须加载自己的 `env/<component>.properties`；`UNIFIED_XINGYUAN_SKIP_BUILD=true` 仅在确认所有现有artifact是当前版本时使用。

直接启动时也必须加载同一受管配置；以下命令由操作者按拓扑顺序执行，前端在各包目录运行 `npm run dev`：

```bash
java -jar egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/target/egon-cola-tianquan-shoubing-admin-exec.jar --spring.config.additional-location=file:/absolute/runtime/env/tianquan-shoubing.properties
java -jar egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/target/egon-cola-tianshu-admin-exec.jar --spring.config.additional-location=file:/absolute/runtime/env/tianshu.properties
java -jar egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/target/egon-cola-tianquan-jianshen-admin-exec.jar --spring.config.additional-location=file:/absolute/runtime/env/tianquan-jianshen.properties
java -jar egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/target/yuheng-admin-exec.jar --spring.config.additional-location=file:/absolute/runtime/env/yuheng-admin.properties
java -jar egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway/target/yuheng-biz-gateway-exec.jar --spring.config.additional-location=file:/absolute/runtime/env/yuheng-biz-gateway.properties
java -jar egon-cola-xingyuan/egon-cola-yuheng/yuheng-mcp-gateway/target/yuheng-mcp-gateway-exec.jar --spring.config.additional-location=file:/absolute/runtime/env/yuheng-mcp-gateway.properties
```

```bash
scripts/unified-xingyuan/status-local-stack.sh
scripts/unified-xingyuan/verify-local-stack.sh
```

## 人员与机器授权边界

Portal/Admin通过Edge配置的Code/S256流程登录，可信AS页面负责密码策略，浏览器仅持会话cookie。高风险角色操作需要真实OIDC step-up。不得注入USER token或tenant默认值到前端，不把SERVICE token当作USER授权材料。

机器控制面注册使用PLATFORM registration；业务鉴神凭据按可信tenant选择显式TENANT registration。来源app、Resource、最小scope、Tianshu HMAC和registry准入一起保留。MCP Provider拿到的Bearer必须绑定其精确Resource，不能借用Mock或另一个MCP角色的audience。

本地USER输入由操作者通过正式授权取得，以私密文件引用提供。工具先做metadata早期拒错，再调用配置issuer的正式UserInfo校验；本地解码不是签名/授权证明。过期或缺少输入时停止，不自动登录/刷新；撤销验收额外提供已撤销的独立USER证据。

## 深度验收与限制

用户运行验收仍需覆盖：

- 鉴神角色激活/撤销和两tenant隔离；共享Redis不可用时fail-closed。
- 天枢HMAC、注册、配置LKG与恢复；MCP角色和来源应用匹配。
- 注解托管Operation、Stable/RC/Legacy SSE、Tools/Resources/Templates/Prompts/Completion/Apps和订阅。
- A创建会话/任务，B读取或恢复；PostgreSQL持久任务和Redis事件/会话流。
- 本地Operation、远端MCP联邦、超时/熔断/恢复及流式取消。
- 真实Edge Code/PKCE、session/CSRF/logout、RT轮换、单飞refresh、密钥多实例传播和切流后旧根410。

CLI不会代替浏览器refresh/logout验收。缺少相关材料或运行证据必须明确记录，不能以静态测试/HTTP匿名预检宣称完整运行通过。联调验收可能短暂中断并恢复天枢、Remote MCP或权限状态；先阅读所执行脚本并保留用户现场。

官方MCP conformance由操作者按仓库部署脚本运行，Stable/RC场景与实际联邦夹具不能混用；结果保留在当前runtime证据目录。不要打印密码、client secret、AT、RT或Cookie内容。

## 停止和排查

```bash
scripts/unified-xingyuan/stop-local-stack.sh
```

仅处理受管PID，保留数据库/Redis、secret、spool、日志和证据。缺注册配置/MP事实时回受控准备；issuer/credentialVersion不匹配时核对正式注册，不能恢复旧根协议或复制旧hash。USER过期时由操作者重新正式授权；MCP B问题先核对角色artifact、18184/18183和独立instance ID。真实设施或浏览器问题需要用户运行证据。

## 本任务独立隔离入口（Step17 实施中）

新入口是 `scripts/unified-xingyuan/isolated-integration.sh`，准确输入、九命令顺序、错误与恢复合同见[本机手册第7节](../runbooks/unified-identity-local.md#7-新隔离接入-harnessstep17-实施中)。它不会复用本页旧 181xx 栈；任务 MCP 为28185/28186，API Engine 为28180/28182，Edge 为28200，任务 nginx 提供固定 HTTPS28443 同源入口。任务 PG 九库及 Redis DB1–15 必须先通过 owner/OID/manifest 守卫，禁止 Docker。

先 prepare → provision-identity → start-control-plane → provision-authorization/platform，再由 authorize-cli 取得真实 Tianshu USER 输入；原生目录/角色/字段规则及 snapshot READY 后，按审核 registry 取得每个资源、两租户的独立 PUBLIC 输入，才允许 start-consumers。native phase 和两个完整组合 runner 已接线；默认关闭，真实设施与跨进程验收仍须 Step18 明确 opt-in。下游未启动的503或输入缺失不能改成成功。

MCP 运行仍保留真实 SERVICE registration、精确 Resource/scope、HMAC、目录准入、PDP 和审计 readiness。PUBLIC 输入生成只校验真实 AS/UserInfo，`probeStatus:null` 不证明 MCP/HTTP/RPC 权限。完整 verify 必须执行实际资源请求、Scope∧PDP、字段 WRITE、两 tenant SQL 隔离及原 owner 的真实撤销证明；没有外部 LLM 输入时不宣称推理已验收。

五个逻辑 BFF alias 在 A/B 两个真实 issuer 下必须各自对应真实 CONFIDENTIAL 客户端与独立 AT/RT。浏览器按 A 全部退出→显式 B 新授权→B refresh/step-up→B 全部退出执行；原 AS owner 证明各族同 AT 退出前200、未自然过期的退出后401。PUBLIC CLI 族的独立撤销测试不能替代该证明。

status 只读；stop 只处理当前 manifest/ledger 逐项匹配的任务进程并保留数据、secret、spool 与证据。原 DB OID/Redis namespace/nginx PID及端口的保留核查仍是最后 LIVE-023 门禁。当前只有源码/受控检查，真实 MCP/browser/PG/Redis/跨进程验收尚未执行。

原设施before文件original-databases.json、original-nginx.json、original-redis.json都必须保留。Redis检查使用Redis 7+只读脚本，记录非任务库原键类型、值摘要和绝对到期时间，允许自然过期、拒绝提前丢失/值或TTL改变；超限/不可读明确失败，不更改ACL或清库。最终三项均在owned stop后复读，任何单项proof不能替代完整LIVE-023；容量、指纹与观测局限见[联调手册](../runbooks/unified-identity-local.md#7-新隔离接入-harnessstep17-实施中)。

原生目录准备的独立测试入口IsolatedAuthorizationProvisioningIT#provisionNativeMetadataCatalog已接入原天枢HTTPS28150 bizs/apps API：使用先前CLI006正式PUBLIC/USER天枢输入，经服务端原鉴权创建并回读实际目录ID。首次POST前记录私有evidence/native-metadata.json RUNNING；未知写不盲重试，COMMITTED只允许显式resume只读复核；不修改已有目录，不猜ID。该入口默认禁用，现由CLI004native完整owner链按序调用，不能将独立子阶段当作原生授权成功或直接跳到启动消费者；真实调用留待完整Step17门禁后执行。

原生目录到鉴神的独立入口IsolatedAuthorizationProvisioningIT#provisionNativeCatalogReview已接线：要求metadata与平台授权均已COMMITTED，真实Directory/membership及天枢目录归属有效；原owner对两tenant准入四应用，再注册、显式审核permission、配置映射与字段定义。租户准入记录ID与全局应用ID分别处理。首次写前保存evidence/native-catalog-review.json RUNNING，完整回读后才COMMITTED；中断不盲重跑，COMMITTED显式resume只读核对目录/映射/准入摘要。它不创建角色/grant/assignment/activation，不重新授予平台权限，也不代表CLI004native完成；该真实入口默认关闭，仍需完整Step17闭环后联调。


任务 MCP 复用原玉衡 Resource URI、正式 USER 输入与原 `LOCAL_OPERATION`。唯一任务 server 为 `xingyuan-it-<runUUID>`，内部资源 URI 为 `egon://xingyuan-it-<runUUID>/scopes`，实际调用已发现的玉衡 scopes GET；HTTPS 业务地址不能用作内部 URI。原 profile 的既有 `egon.cola.platform.tianquan.jianshen.system-code` 必须显式为当前 registry 的实际 `yuheng-admin`，不能保留 `mock-backend`。

full 先完成六项 Biz scope/完整性故障和原 owner 恢复，再发布精确二 policy/一 route/一 server/一 resource。唯一细权限为 `mcp:xingyuan-it-<runUUID>:resource:scopes:read`，通过原鉴神 PermissionCatalog、catalog/mapping、Role/Grant/Assignment/Activation owner 准备，只给两租户原 admin 任务角色，保留旧 active roles，viewer 不授。审核模板、SHA/private mode、异步观测计数和未知结果处理见[本机手册第7节](../runbooks/unified-identity-local.md#7-新隔离接入-harnessstep17-实施中)的 `policies/mcp-authorization.json` 说明；不增加公开 CLI 命令或生产 scope/Client/Grant。

initialize 只建立会话，实际验收调用 `resources/read`：正例/恢复各进入原业务一次，缺 scope、缺细权限、错 audience、裸请求业务零调用。审计成功转发有 MCP 与玉衡两个 USED，403 为 USED+DENY，PG/Stream/签名 STORED receipt 与原 spool 清理逐条关联；20个实际事件与28个候选路径分别记录，签名协议无 receiver 字段时不补造归属。此处只有源码和受控门禁，实际运行仍归 Step18。

## Step19 审计后的实际状态（2026-10-16）

本手册的RPC/MCP运行流程与深度验收**本轮未执行**。Step18真实隔离运行经用户于2026-10-16明确批准跳过（本机缺少经人工核对的`intent-manifest.json`、凭据/secret文件与两租户真实registry导出）。第92节的本任务隔离入口源码已随Step17（e99c004d7）提交，其契约回归在2026-10-16实测exit0。

当前未验证项：`resources/read`的真实正例与恢复、每租户业务一次进入、缺scope／缺细权限／错audience／裸请求的业务零调用、MCP与玉衡两次独立验签的两个USED、403的USED+DENY、PG／Stream／签名STORED receipt与原spool清理的逐条关联、20个实际事件与28个候选路径的比对，以及真实多issuer MCP/PG/Redis验收。唯一细权限`mcp:xingyuan-it-<runUUID>:resource:scopes:read`未创建，二policy／一route／一server／一resource未发布，两租户admin任务角色未授予，旧active roles未改动。

未启动nginx、PostgreSQL、Redis、玉衡或任何MCP服务进程，未改动本机TLS或端口占用。源码与受控门禁的结论见[实施Plan §12.128](../egon/plan/2026-10-11-12-03-xingyuan-native-full-integration-plan.md)；真实运行验收仍归Step18，不能以受控门禁或模拟替代。
