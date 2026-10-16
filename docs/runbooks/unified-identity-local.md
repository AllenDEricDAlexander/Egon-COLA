# 统一身份平台本机联调手册

本手册对应新的按 issuer 隔离的 SAS/OIDC 身份链。浏览器使用 Edge 的服务端 BFF Session 和 HttpOnly session cookie；Portal/四个 Admin 通过 Authorization Code + S256 PKCE 登录。浏览器不保存 USER AT/RT，Yuheng Engine 接收 TokenRelay 后的 Bearer 并继续本地验签、共享撤销检查和鉴神授权。

所有运行命令由操作者执行。编码工作只做源码、静态和内存测试，不自动启动应用、PostgreSQL、Redis、Edge、浏览器、Docker或导入数据。

## 1. 前置条件

- Java21、仓库 Maven Wrapper，以及 `curl`、`jq`、`openssl`、`psql`、`createdb`、`redis-cli`、`awk`、`nc`；完整平台还需要 Node/npm。
- 操作者已运行并配置 PostgreSQL、带密码的 Redis 和独立 Edge；脚本复用这些设施，不启动/关闭它们。
- 已完成[受控导入和切流流程](unified-identity-oauth-client-tenant-cutover.md)，或通过正式管理流程准备当前 MP identity/user/tenant/membership、realm、3072-bit signing key、SERVICE client、Resource、Grant。旧 `public.identity_*` 表保留只读，不能充当新部署目录。
- 鉴神已有真实 tenant-authority 迁移状态；脚本不会构造假的 `VERIFIED` JDBC 参数，也不会在启动时执行旧 tenant-authority import。
- 每个本地 SERVICE client 在 PLATFORM 和两个业务 tenant issuer 中分别注册。它们可以使用同一个显式 client ID，但 issuer、secret 和 credentialVersion 必须独立配置。
- 操作者通过正式 OIDC/CLI 授权取得两个 tenant 的短期 USER 输入文件，并预先授予所需 Resource/Scope/角色。它们是操作者提供的 CLI 材料，不能从 BFF session/cookie 提取。缺失、过期或无权的输入会停止相应阶段。

首次使用通过私密文件提供数据库凭据，不写进仓库或命令历史：

```bash
export UNIFIED_IDENTITY_POSTGRES_PASSWORD_FILE=/absolute/private/postgres.password
export UNIFIED_IDENTITY_REDIS_PASSWORD_FILE=/absolute/private/redis.password
export UNIFIED_IDENTITY_OAUTH_REGISTRY_FILE=/absolute/private/local-oauth-registry.json
export YUHENG_EDGE_BASE_URL=http://127.0.0.1:<operator-edge-port>
```

注册配置的精确格式见[当前修订 Spec §16.1](../egon/spec/2026-10-08-10-18-oauth-current-code-alignment.md)。配置必须为当前用户所有的绝对普通文件、非 symlink、mode400/600、≤128KiB；其中只有标识和私密文件引用，不能嵌入 token、密码、secret 或私钥。ID/version 用十进制字符串，避免 JSON 工具丢失 Long 精度。

配置包含 publicOrigin、目录 P partition、平台管理 tenant、identitySub、revocationEpoch、KEK/audit-HMAC 文件，以及有限的 issuer/tenant/client map。每个 client 的 `platform` 和 `tenants[tenantId]` 指定 `issuerRef`、`credentialVersion`、`secretFile`。现有本地 client ID 必须全部明确列出：

- `tianquan-shoubing-service`、`tianquan-jianshen-service`、`tianshu-service`。
- `yuheng-admin-service`、`yuheng-biz-gateway-service`、`yuheng-mcp-gateway-service`。
- `mock-backend-service`、`mcp-provider-service`。

Spring 默认 registration `egon-tianquan-shoubing` 对应 PLATFORM；`egon-tianquan-shoubing-tenant-<id>` 对应该业务 tenant，鉴神通过 `service-client.tenant-registration-ids` 选择它。不得用 PLATFORM token 代替 USER 或业务 tenant SERVICE token。

## 2. 准备、启动和验证

以下命令会实际访问设施或启动进程，只由操作者在前置准备完成后运行：

```bash
./scripts/unified-identity-local.sh prepare
./scripts/unified-xingyuan/start-local-stack.sh
```

`prepare` 校验私密注册配置，创建尚不存在的具名开发数据库，生成受保护的运行配置并构建 JAR。它不进行身份/OAuth 数据导入或通过旧协议登录。`start` 在启动任何依赖进程前只读核查当前 MP directory、realm/key/client/credential/Resource/Grant；首先启动已经配置的授柄 AS，再启动天枢与其余服务。AS 的显式 local registration factory 使用现有可信 endpoint，不在自身监听器创建前进行自发现。

默认数据库仍为 `egon_identity_local`、`egon_rbac3_unified_identity_local`、`egon_gateway_local`、`egon_tianshu_local`。既有数据库和数据不清理。空库上缺少迁移/配置事实时应停止，并先完成独立的受控准备。

完整平台的 `prepare-local-stack.sh` 会运行用户授权的本地编排并停止受管进程；不要把它当作静态检查，也不要在缺少前置材料时执行。生成的 `.env.local` 只含 `VITE_YUHENG_ORIGIN=<Edge origin>`，不注入 tenant/password。后端 `env/*.env` 与 `*.properties` 为mode600，实际启动会加载相应 Spring properties。

如需单独运行某个已有配置的 JAR，显式加载它自己的配置并遵循已核实的运行顺序。例如：

```bash
java -jar <component-executable.jar> \
  --spring.config.additional-location=file:/absolute/runtime/env/<component>.properties
```

已有配置下，各组件执行文件如下；操作者应为每条命令填写其自己的绝对 properties 路径：

```bash
java -jar egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/target/egon-cola-tianquan-shoubing-admin-exec.jar --spring.config.additional-location=file:/absolute/runtime/env/tianquan-shoubing.properties
java -jar egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/target/egon-cola-tianshu-admin-exec.jar --spring.config.additional-location=file:/absolute/runtime/env/tianshu.properties
java -jar egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/target/egon-cola-tianquan-jianshen-admin-exec.jar --spring.config.additional-location=file:/absolute/runtime/env/tianquan-jianshen.properties
java -jar egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/target/yuheng-admin-exec.jar --spring.config.additional-location=file:/absolute/runtime/env/yuheng-admin.properties
java -jar egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway/target/yuheng-biz-gateway-exec.jar --spring.config.additional-location=file:/absolute/runtime/env/yuheng-biz-gateway.properties
java -jar egon-cola-xingyuan/egon-cola-yuheng/yuheng-mcp-gateway/target/yuheng-mcp-gateway-exec.jar --spring.config.additional-location=file:/absolute/runtime/env/yuheng-mcp-gateway.properties
```

不能依赖旧的无参数示例在新配置下自动创建 issuer/key/client。不能将历史 PEM 文件用作新的全局 OAuth signer。

## 3. 端口与角色

| 组件 | 默认地址/端口 | 角色 |
| --- | --- | --- |
| 授柄 AS/Admin | 18120，内部RPC18122 | `/platform` 和 `/tenants/<id>` issuer、身份管理 |
| 鉴神 Admin | 18130 | 角色/权限/授权快照 |
| 玉衡 Admin | 18140 | 控制面 |
| 天枢 Admin/RPC | 18150/19080 | 注册/配置/HMAC |
| 玉衡 API Engine | 外部18180，管理18182 | HTTP/RPC 数据面 |
| 玉衡 MCP Engine A | 数据18185，管理18186 | 独立 MCP 数据面 |
| MCP Engine B | 数据18184，管理18183 | 同一MCP角色的第二节点；PID/env兼容别名为 `yuheng-biz-gateway-b` |
| Mock/MCP Provider/Remote MCP | 18160/18161/18151 | 本地受保护夹具 |
| Portal/四个 Admin Web | 18125/18121/18131/18141/18152 | 浏览器 OIDC/BFF 入口 |
| Edge | 操作者指定 `YUHENG_EDGE_BASE_URL` | 独立 SCG/BFF；脚本不启动它 |

## 4. 人员登录与 CLI 输入

操作者运行对应前端 `npm run dev` 后，选择登录，浏览器导航至 Edge `/oauth2/authorization/edge-ui`，再由可信 tenant AS 完成身份校验。Portal/Admin 不收集密码或读取 AT/RT。角色高风险操作使用真实 OIDC step-up；pending role intent 由既有流程恢复。

本地工具的 `oauth-context` 验证受保护的正式 USER 输入后只输出 tenant/subject/issuer；`issue-user-token` 为兼容命令名，其行为是验证并复制操作者的正式输入到明确的受保护输出，**不会签发、登录、刷新或读取浏览器 cookie**。

```bash
UNIFIED_IDENTITY_TENANT=default ./scripts/unified-identity-local.sh oauth-context
UNIFIED_IDENTITY_TENANT=tenant-b \
UNIFIED_IDENTITY_ACCESS_TOKEN_FILE=/absolute/private/verified-mcp-user.jwt \
  ./scripts/unified-identity-local.sh issue-user-token
```

不要打印材料文件内容，不把它写进前端存储。到期时由操作者重新完成正式授权并更新受保护输入；后台脚本不会取得或续用浏览器 RT。

## 5. 验收与证据

```bash
./scripts/unified-identity-local.sh verify
./scripts/unified-xingyuan/verify-local-stack.sh
```

后端验收保留角色激活前403/后200、同主体两tenant隔离、原授权快照、Tianshu注册/HMAC、Yuheng发布和MCP能力检查。撤销测试额外需要 `tenants.default.revokedAccessTokenFile` 指向操作者通过正式流程产生并撤销的独立 USER 证据；缺少时明确停止。CLI 不操纵浏览器会话或 RT。

浏览器验收另外验证 Authorization Code/S256、HttpOnly session、`/bff/session`、CSRF logout、真实 step-up、单次 refresh/RT轮换和远程撤销结果。旧根 OAuth十个操作在新部署返回410；没有根 password/token/JWK 双链。必须记录真实PG/Redis、跨节点撤销/密钥传播、SSE/MCP、浏览器运行结果；源码或模块测试不能代替它们。

## 6. 状态、日志和停止

```bash
./scripts/unified-xingyuan/status-local-stack.sh
./scripts/unified-xingyuan/stop-local-stack.sh
```

运行目录包含受保护 `env/`、`secrets/`、`pids/`、`logs/`、audit spool 与验收证据。直接使用底层工具时默认目录为 `.runtime/unified-identity/`；完整平台为 `target/local-unified-xingyuan/`。只停止由当前工具记录的存活PID；PG/Redis/Edge、数据库、密钥、日志和用户证据均由操作者保留。

## 7. 新隔离接入 harness（Step17 实施中）

`scripts/unified-xingyuan/isolated-integration.sh` 是本任务独立入口，输入合同见[Runtime Spec R1–R6](../egon/spec/2026-10-11-10-26-xingyuan-native-full-integration-runtime.md)。它使用 `target/xingyuan-integration-<runId>`，不读取上一节的旧运行目录。原生授权 phase 已接入真实actor、角色分配、临时管理策略关闭、显式激活及owner回读；native/resource 两个完整组合入口现已有源码，默认不会连接运行设施。实际验收须在 Step18 明确 opt-in 后执行，源码检查不算运行接入完成。Plan §12.55 的独立 provisioner 方案已于2026-10-15获用户批准，其源码与默认守卫已实现；§12.49/12.50 支持空库中未使用系统初始行的目录/应用编码绑定。

操作者先按 R1 准备完整 manifest、实际可执行制品及其 SHA、审核 catalog/policy 文件、十二份 Java profile 模板和任务 TLS。脚本不生成占位 manifest 或猜测业务 ID。manifest/配方/口令/令牌/配置文件必须当前用户所有、mode600；任务目录及私有子目录 mode700，禁止符号链接。密码、私钥与令牌通过受保护文件引用提供，不写命令行、stdout、浏览器存储或仓库。

隔离连接固定为 PostgreSQL `127.0.0.1:5432`、Redis `127.0.0.1:6379` 的一个显式 DB1–15（禁止 DB0），公共同源 `https://127.0.0.1:28443`。九个新库分别为 `xingyuan_it_{idp,rbac,tianshu,yuheng,llm,light,web,service,agent}_20261011`，每个库都要求本任务 owner/OID/COMMENT 凭据。这里的大括号表示九个独立名称，不是 manifest 值。旧库、旧 Redis namespace、系统 nginx 和 Docker 均不参与任务创建/停止。

以下配置值须替换成当前已审核任务的绝对路径和 registry 中的真实 resourceKey；`XINGYUAN_RUN_ROOT` 必须与 manifest 完全一致：

```bash
export XINGYUAN_RUN_ROOT="/absolute/repository/target/xingyuan-integration-<runId>"
export XINGYUAN_INTEGRATION_MANIFEST="$XINGYUAN_RUN_ROOT/manifest.json"
export XINGYUAN_TIANSHU_RESOURCE_KEY="<reviewed-tianshu-resource-key>"
export XINGYUAN_ISOLATED_INTEGRATION=true
```

每次调用使用独立 `--result` 文件；固定阶段凭据由脚本另行发布。以下命令会创建任务数据或启动任务进程，只由操作者显式执行，不属于默认测试：

```bash
scripts/unified-xingyuan/isolated-integration.sh prepare --manifest "$XINGYUAN_INTEGRATION_MANIFEST" --result "$XINGYUAN_RUN_ROOT/evidence/operator-prepare.json"
scripts/unified-xingyuan/isolated-integration.sh provision-identity --manifest "$XINGYUAN_INTEGRATION_MANIFEST" --result "$XINGYUAN_RUN_ROOT/evidence/operator-identity.json"
scripts/unified-xingyuan/isolated-integration.sh start-control-plane --manifest "$XINGYUAN_INTEGRATION_MANIFEST" --result "$XINGYUAN_RUN_ROOT/evidence/operator-control.json"
scripts/unified-xingyuan/isolated-integration.sh provision-authorization --phase platform --manifest "$XINGYUAN_INTEGRATION_MANIFEST" --result "$XINGYUAN_RUN_ROOT/evidence/operator-platform.json"
scripts/unified-xingyuan/isolated-integration.sh authorize-cli --manifest "$XINGYUAN_INTEGRATION_MANIFEST" --tenant-code default --resource-key "$XINGYUAN_TIANSHU_RESOURCE_KEY" --user-key admin --output-file "$XINGYUAN_RUN_ROOT/secrets/formal-tianshu-default-admin.at" --result "$XINGYUAN_RUN_ROOT/evidence/operator-tianshu-input.json"
```

控制面启动先经原天枢 owner 准备真实基础目录，再启动 AS/任务 nginx 并检查 canonical discovery/JWKS，然后天枢、鉴神、玉衡 Admin 和 Edge 依各自真实依赖启动。`platform` phase 经原 owner 建立授权和真实 snapshot/version；目录登记或资源注册成功本身不等于权限可用。

下列三条是后续源码入口。`native` 和 `start-consumers` 已接线但尚未实际运行，缺少审核材料应退出4；`verify` 当前缺少两个完整runner，须在任何验收子进程启动前退出4，不能以局部探针或受控测试结果绕过：

```bash
scripts/unified-xingyuan/isolated-integration.sh provision-authorization --phase native --manifest "$XINGYUAN_INTEGRATION_MANIFEST" --token-index "$XINGYUAN_RUN_ROOT/secrets/token-index.json" --result "$XINGYUAN_RUN_ROOT/evidence/operator-native.json"
# native 授权完成后，按审核 registry 为每个资源、两个租户和 admin/viewer 分别执行 authorize-cli；不能借用另一个资源的 AT。
scripts/unified-xingyuan/isolated-integration.sh start-consumers --manifest "$XINGYUAN_INTEGRATION_MANIFEST" --token-index "$XINGYUAN_RUN_ROOT/secrets/token-index.json" --result "$XINGYUAN_RUN_ROOT/evidence/operator-consumers.json"
scripts/unified-xingyuan/isolated-integration.sh verify --manifest "$XINGYUAN_INTEGRATION_MANIFEST" --token-index "$XINGYUAN_RUN_ROOT/secrets/token-index.json" --case-set full --result "$XINGYUAN_RUN_ROOT/evidence/operator-full.json"
```

`authorize-cli` 使用真实 AS Code/S256、nonce/state、精确 issuer/client/resource、签名及 UserInfo 校验，PUBLIC 无 RT。`probeStatus:null` 表示尚未请求业务资源；它不证明 PDP、字段 WRITE 或 SQL 隔离。到期后由操作者重新完成正式授权，不能导出 BFF AT/RT、伪签令牌或自动刷新浏览器令牌。

`verify` 必须核对 24 项 LIVE、三项真实 JPA/MP Outbox producer 测试零跳过、真实 A/B 浏览器旅程及实际 BFF 族 owner 证明。PUBLIC 族撤销另由同 AT 摘要限定的 SA 原 owner 执行，再要求相同未过期 AT 返回401；它不能替代 BFF logout。缺 runner、证据、输入、投影或 readiness 均不得报告完整 PASSED。真实设施/browser/RPC 验收目前未执行。

```bash
# status 为只读查询；stop 需要上面的显式 opt-in。
scripts/unified-xingyuan/isolated-integration.sh status --manifest "$XINGYUAN_INTEGRATION_MANIFEST" --result "$XINGYUAN_RUN_ROOT/evidence/operator-status.json"
scripts/unified-xingyuan/isolated-integration.sh stop --manifest "$XINGYUAN_INTEGRATION_MANIFEST" --result "$XINGYUAN_RUN_ROOT/evidence/operator-stop.json"
```

停止前逐项验证 ledger 中 PID、启动时间、命令和制品摘要，只向实际归属本任务的进程发 TERM，保留库、secret、日志和证据。旧设施保留还必须由 LIVE-023 对原 DB OID、Redis namespace、nginx PID/端口的前后核查证明，单凭任务 PID 停止不算通过。

退出码：2 输入错误；3 不安全目标；4 缺材料/实现；5 状态冲突；6 依赖不可用；7 部分完成；8 验证失败；9 提交状态未知；10 进程归属不符。`--resume` 只支持 prepare、provision-identity、provision-authorization，且必须是同 run/hash 的明确恢复。UNKNOWN_COMMIT 必须先只读核实 owner 状态，不盲目重做授权、rotate 或撤销；禁止 DROP、FLUSHDB、Flyway repair 和修改历史迁移。

原生局部源码目前包含 Light API/只读 SQL 隔离和四原生配置的 SERVICE 缓存探针。缓存探针按真实 `expiresAt/renewalSkew` 等待，可能持续数分钟；其结果只证明该 SDK/AS 缓存观察，不能替代运行应用的 HTTP/RPC、AS 审计或完整矩阵。父编排现在在长浏览器与缓存观察完成后，通过明确的 `FORMAL_CODE_INPUTS` 阶段逐项再执行原 CLI006 正式 PUBLIC Code 流程；仍读取本任务已批准的 admin/viewer 私有口令引用，不续用 BFF RT，不扩大 scope/grant。新输入使用独立私有文件，旧文件保留，逐项 CLI006 凭据与汇总记录保存在当前 verify 证据目录。提交未知、换身份、扩大 scope 或复制旧 AT 均停止。完整矩阵和实际时长仍未验证，不能缩短真实 token TTL、修改时钟或接受过期输入来制造通过。

RPC 局部源码另通过审核制品的真实 facade/Protobuf 契约、原 HMAC 目录和身份拦截器检查双向协议；`native-rpc-contract-probe.json` 的 `STANDALONE_NOT_FOUND_PROTOCOL` 只表示缺行协议探针范围，不能当业务数据正例或 handler 零调用证明。离线生成制品检查只加载契约、构造原消费者代理，禁止获取 Channel；它不启动 provider，也不等于实际 RPC 联调通过。

完整verify会先记录真实四业务BFF窗口的等待下界；例如四业务族均600秒时A/B至少1082秒，900秒任务在验收子进程前拒绝。不要用缩短实际TTL或修改时钟绕过。Service→Web另有原API写入后通过实际RPC按返回ID读取的正例源码，其STANDALONE_SERVICE_TO_WEB_DATA证明仍属独立消费者范围，当前未实际运行。

Light GraphQL另有基于同次REST/SQL证明行ID的两tenant/双alias源码检查；先运行真实数据准备，不能伪造native-tenant-isolation证明。LIGHT_GRAPHQL_TWO_TENANT_READ结果只覆盖该局部读取路径，其他roots、异步/cancel和拒绝审计仍独立必需，目前没有真实GraphQL运行结论。

Agent知识元数据另有两tenant实际API create/get/list和低权拒绝的源码入口，读取当前profile的logical embedding model，不为该检查调用模型。AGENT_KNOWLEDGE_METADATA_ONLY不能解释为文档ingest、异步Outbox或推理成功；当前真实入口仍未运行，任务数据与失败现场保留。

prepare现在在创建任务库前保存原数据库名称/OID/owner基线，恢复时核对，full在任务停止后再次只读检查。不要删除或重写original-databases.json；缺失before不能从已修改后的状态重建。ORIGINAL_DATABASE_IDENTITIES_ONLY仅证明此维度，Redis/nginx及旧表内容不能由它推断，真实PG保留检查目前未运行。

prepare也保存原nginx进程/监听基线，恢复与最终停止后只读复核，不reload/停止系统nginx。不要修改original-nginx.json；权限不足或进程身份无法核实时必须停止，不能填空基线。ORIGINAL_NGINX_IDENTITIES_AND_PORTS_ONLY不证明配置内容或Redis保留，真实验收目前未运行。

完整验证的任务nginx审核配置还须包含以下固定故障开关。`<runtimeRoot>`替换为manifest中的绝对路径，保留现有任务TLS、location和upstream；配置SHA必须在manifest冻结前确定。prepare仅初始化mode600的OFF map（`default 0;`），不为运行中的未知故障倒造初始状态：

```nginx
http {
    map $uri $xingyuan_task_revocation_fault {
        include "<runtimeRoot>/nginx/revocation-fault.map";
    }
    server {
        if ($xingyuan_task_revocation_fault) { return 503; }
        # 本任务原有TLS、location与upstream配置。
    }
}
```

浏览器源码只通过内部固定命令启用实际三issuer撤销路径故障，核503/PENDING、本地退出、原队列持久intent和晚到callback，再finally恢复OFF并等原worker确认；原AS内存oracle继续核同批未过期AT的401及业务计数不增长。队列观测只读任务stream数量，不读或解密载荷。九个公开命令不变，受控shell测试通过不等于这段真实旅程已执行。父full会在验证子进程前拒绝无开关或非OFF map，并为真实队列恢复另留90秒预算。

若实际运行被中断并留下故障，先以原status核对任务进程归属，再显式执行以下内部恢复入口；结果文件须是新的唯一私有路径。它只恢复同run/hash、实际owned任务nginx，未知结果继续保留材料，不能重试ON或操作系统nginx：

```bash
python3 -I scripts/unified-xingyuan/lib/isolated_integration.py revocation-fault "$XINGYUAN_INTEGRATION_MANIFEST" "$XINGYUAN_RUN_ROOT/evidence/operator-restore-<unique>.json" off
```

玉衡spool故障源码只在本任务`evidence/oauth-spool-yuheng`新建自身唯一IO marker/容量文件，核原readiness DOWN、API503和原业务计数不增长，finally按inode/owner等身份校验移除自身文件并核恢复UP/200。不得删除真实.evt/.sent/.dead或借全局Redis停机制造故障。IO marker测试与原签名DEAD receipt受控测试是两种证据，目前均不能替代Step18真实运行验收。

四平台HTTP局部源码现会把实际响应中的W3C trace写入安全metadata，并经内部固定SAA原owner只读核对PG、原stream和签名STORED receipt，等待相应宿主原frame/sent消失。原签名保留wire纳秒，PG字段按实际managed JDBC的微秒精度核对，不能用PG时间重建HMAC后误判receipt无效；原BFF族oracle也复用此核验。新的内部桥只计本次实际owner测试和这些局部请求，不增加公开CLI或代替完整LIVE021。真实链尚未运行。

原生RPC已有局部入口另核原provider的业务计数：成功调用只增长对应get，匿名UNAUTHENTICATED调用前后所有方法计数相等。认证后的跨租户缺行属于真实业务读取，不计入匿名零handler证明。实际SERVICE read-scope调用write方法的拒绝，以及原AS ACCESS/JTI撤销后的同一未过期AT拒绝，均已有原owner/业务计数源码；完整LIVE014仍需组合runner闭环及Step18实际验收。

prepare在首个任务CREATE/Redis claim前还保存私有original-redis.json，复用固定127:6379和保护口令，以Redis 7+只读EVAL_RO扫描非任务逻辑库；恢复、准备收尾和最终任务停止后检查原键类型、DUMP摘要及精确绝对过期时间。键名/值只在Redis内计算SHA1相等性指纹，不输出明文，不用作签名或授权；证据文件以SHA256绑定。只有原定过期时间已经到达才允许原键消失，提前消失、值/TTL改变、服务run_id改变都失败。外部应用正常写入或Redis编码变化也可能使检查失败，此时不得声称保留成功或自动归因本任务。

采集最多256个非空逻辑库、512个键、每库256页、单值内存/序列化各1MiB、总读取序列化32MiB；原生集合基数至多4096，模块类型或超限明确拒绝，不截断为成功。命令/整体时间均受manifest上限约束。权限不足、不支持只读脚本、缺before而已有任务事实均阻断，不能改ACL、FLUSH、补造基线或重启Redis绕过。ORIGINAL_REDIS_SAMPLED_KEY_VALUES_ONLY仅证明两次有界观测的原键值/到期边界，不能证明观测间连续历史、已自然过期值或旧业务服务健康；实际Redis验收尚未运行。

离线受控检查使用 `bash scripts/regression/test-isolated-integration-contract.sh`，不会启动应用、浏览器或 Docker；其中的假传输、假进程流水线和临时证据只证明脚本合同，不能计入 LIVE 验收。

2026-10-15源码复核：默认215项中184项实际执行通过，31项显式关闭的运行/文件输入入口不计成功；另以既有12个SHA绑定实际制品执行RPC模型字节比对通过。浏览器仅做独立TypeScript检查与Playwright `--list`，发现1条A/B完整旅程，未打开浏览器。旅程现在额外要求A退出后的B canonical绑定引用不同，B的originalCreatedAt不早于A；每个issuer内部仍严格保持原canonical/absolute时间。JSON输入归一化使用UTF-8，保留精确Long、重复键/非有限数拒绝及有界输入输出。

`verifyNativeAuthentication` 与 `verifyResourceAuthorization` 已接线：固定父流程依次执行原 browser/oracle、正式 Code 输入、三项 JPA/MP 事务门禁、字段 owner/consumer、native 与 resource；resource 最后核查23项先前 case 后调用 owned-stop 并复读原设施。所有 case 只能由实际固定方法执行、私有证明和本次 JUnit/Playwright 记录产生；默认关闭入口不计成功。§12.101、§12.102、§12.120、§12.124、§12.125 均已获用户批准；生产纠正分别提交，Step17 源码门禁与实际运行边界以 Plan 最新执行记录为准。Agent 运行仍保留已接受的输入缺口。

缺少required runner时，CLI007在`SOURCE_READINESS`阶段将全部缺失阶段一次写入指定`--result`，状态`FAILED`、错误码`XINGYUAN_INPUT_UNAVAILABLE`、所有执行计数为0，然后退出4。该结果不创建verify attempt/子进程，不覆盖已有`full.json`，也不代表已检查实际运行设施；修复源码后应使用新的结果文件重新执行。

原生目录准备的独立测试入口IsolatedAuthorizationProvisioningIT#provisionNativeMetadataCatalog已接入原天枢HTTPS28150 bizs/apps API：使用先前CLI006正式PUBLIC/USER天枢输入，经服务端原鉴权创建并回读实际目录ID。首次POST前记录私有evidence/native-metadata.json RUNNING；未知写不盲重试，COMMITTED只允许显式resume只读复核；不修改已有目录，不猜ID。该入口默认禁用，现由CLI004 native父流程按顺序调用；此子阶段不能单独当作原生授权成功或直接跳到启动消费者，真实调用留待完整Step17门禁后执行。

原生目录到鉴神的独立入口IsolatedAuthorizationProvisioningIT#provisionNativeCatalogReview已接线：要求metadata与平台授权均已COMMITTED，真实Directory/membership及天枢目录归属有效；原owner对两tenant准入四应用，再注册、显式审核permission、配置映射与字段定义。租户准入记录ID与全局应用ID分别处理。首次写前保存evidence/native-catalog-review.json RUNNING，完整回读后才COMMITTED；中断不盲重跑，COMMITTED显式resume只读核对目录/映射/准入摘要。它不创建角色/grant/assignment/activation，不重新授予平台权限，也不代表CLI004native完成；该真实入口默认关闭，仍需完整Step17闭环后联调。

运行配置准备还可执行文件级盘点IsolatedOAuthProvisioningIT#inventoryRuntimeProfileInputsOffline（显式SystemProperty xingyuan.isolated.profile-inventory.enabled=true）。输入由xingyuan.isolated.profile-inventory-file指向私有任务policies/runtime-profile-artifacts.json，值为完整12角色到现ArtifactReferenceDTO的path/sha256引用；repository-root仍为当前仓库。只解析实际Boot JAR/classpath配置，evidence/profile-inputs/保存每角色配置键/引用名/unresolved标记和SHA摘要，不输出值，不读取宿主optional:file/configtree或tianshu配置，也不生成profiles-ready.json。它用于审核模板准备，不能替代绑定/运行验证；重复同输入报告保持字节不变，差异拒绝覆盖。

Agent原非test启动会初始化实际MCP工具连接，还要求模型/embedding配置。仅跑身份或知识元数据探针也不能用假key/地址跳过启动约束。实际受保护配置路径尚未提供，Agent运行因此仍未验证；外部推理仍须单独输入和验收，不因配置盘点通过而声称完成。

文件盘点命令如下；先替换当前仓库/私有任务路径，并准备十二个实际制品的SHA引用。离线缓存需要专用settings时，只加已审核settings文件的`-s`引用。

```bash
./mvnw -o -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin -am \
  '-Dtest=IsolatedOAuthProvisioningIT#inventoryRuntimeProfileInputsOffline' \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dxingyuan.isolated.profile-inventory.enabled=true \
  -Dxingyuan.isolated.repository-root="<repo>" \
  -Dxingyuan.isolated.profile-inventory-file="<task-root>/policies/runtime-profile-artifacts.json" test
```

预期只执行该文件测试，生成12份requirements及summary，不启动main或监听端口。报告编码固定Map排序；相同输入跨JVM重跑须保持全部13文件SHA不变，差异失败并保留旧证据。不同源码/制品的再次盘点使用新的私有任务目录，不覆盖已有报告。

2026-10-15用户已明确暂不提供Agent外部配置，保留其运行未验证。本轮不补假模型/MCP参数，不启动Agent或调用外部模型；继续其接入源码、离线安全测试和其他平台工作。full矩阵不得把未执行Agent用例记作通过，最终验收须显式列出这项已接受缺口。

配置准备会清理实际egon.yuheng.edge.resources及legacy resource-bindings内置身份Map；通用身份清理保留业务route，Edge发布则按下述任务路由片段重建并核对。完整审核模板必须显式重建真实registry绑定，不能继承示例issuer/client。BFF回调配置使用原Edge固定{registrationId}模板，校验时只用实际registration展开并与AS精确白名单比较。

文件级盘点另生成policies/edge-bff-user-fragment.properties候选：既定五alias×两tenant，共82个标准Properties项，仅引用真实registry事实，不写Client secret值。支持安全的嵌套registration事实选择，secret中${...}仍是字面内容；不支持把secret/subject当配置查找键。片段须与其他host/resource/route/SERVICE配置一起审核形成完整edge-profile.properties，不能单独代替profiles-ready或实际联通验收。

完整Edge profile发布前还会从本次SHA绑定的实际Boot JAR加载原EdgeOAuthProperties/资源BO，执行原Binder、Jakarta和validate，并复核manifest三upstream、公共同源、五alias/两issuer/十Client、Resource/scopes、任务key文件和namespace。不会新增SDK依赖或启动Bean/application；错误配置在发布前拒绝。Gateway业务route、TLS、实际Code/refresh/session与运行可用性仍需独立验收。

文件测试packagedEdgePropertiesBindOfflineWithoutStartingApplications默认关闭；需显式SystemProperty `xingyuan.isolated.edge-profile.enabled=true`，以及repository-root、`xingyuan.isolated.edge-profile-artifact`（实际Edge Boot JAR绝对路径）、`xingyuan.isolated.edge-profile-artifact-sha256`。它使用普通合成配置测试实际制品模型，执行非0/零skip才是文件绑定证据，不能声称真实Client或服务已验证。


配置盘点同时输出 `policies/edge-host-resource-fragment.properties`：五alias的业务path/route-id/return-path取自实际Edge制品，Resource ID/URI/scopes及issuer映射取registry，三upstream和session key/namespace取任务引用。候选共64项；仍须补齐route、TLS和SERVICE等完整配置，不单独生成profiles-ready。

公共 `policies/runtime-trust-fragment.properties` 供各Java角色复用，包含PLATFORM与两个租户的原SDK信任配置，不含私密值。审核后纳入每角色完整模板；生成profile时用原IdpStarterProperties逐条验证，并与registry中的issuer/JWKS/realm/tenant/version精确比较，额外/缺失issuer或单issuer回退会拒绝发布。PLATFORM没有tenant-id。此文件门禁不访问JWKS或启动服务。


`policies/edge-routes-fragment.properties` 仅发布本任务登记alias对应的原平台路由与AS协议路由。当前实际制品派生7条路由、33项配置，保留原ID/path/alias/EdgeResourceAuthorization；未登记的biz/MCP/LLM BFF路由不发布。路由转发地址使用manifest角色端口，Resource URI仍是独立逻辑audience；不能把audience当作网络端点。完整Edge模板须纳入此片段，发布前对实际制品和manifest逐项比较路由，错端口/额外路由/改path/缺filter均拒绝。不改变生产路由文件，也不代表真实SCG监听或浏览器链已验。


同一文件盘点还生成12份 `policies/<role>-audit-fragment.properties`（每份15项）。各角色共享本任务的 audit stream 和受保护 audit-hmac 文件，spool 分别位于 `<task-root>/evidence/oauth-spool-<role>`；原组件在实际启动时创建目录，配置预检本身不创建或清空 spool。receiver 使用原组件枚举，四非Open应用均为 APPLICATION。吊销配置绑定本任务 loopback Redis、STRICT_SHARED、500ms 和 runId epoch；观测 fallback 取 registry 中的真实默认租户 issuer。

完整角色配置须纳入相应片段。发布前绑定原审计、观测、delivery、吊销 Properties 并验证任务归属、HMAC 文件恰好32字节及文件/父目录权限。旧 stream/epoch、别的角色 spool/receiver、symlink 或不安全文件权限会拒绝；已有 spool 只读检查并保留内容。该检查不连接 Redis、不启动审计消费者，不代替真实审计送达、receipt 或 spool 恢复验收。Agent 片段仅是离线配置准备，运行未验证决定保持不变。


Biz/MCP 的端口按 Runtime R5.1 分开：公共 HTTPS 分别为28180/28185，Spring 管理 HTTPS 为28182/28186；附加端口在这两角色中不是 RPC。配置盘点生成 `policies/biz-listener-fragment.properties`（12项）和 `policies/mcp-listener-fragment.properties`（11项），将原公共监听器绑定 loopback 主端口及任务 TLS 文件，管理入口使用附加端口。Biz 在本任务不启用未声明的内部 HTTP/RPC 监听器，生产配置保持原样。

CLI-005 的 liveness/health/readiness 探针访问管理端口，其他 JAVA 角色仍访问各自主 HTTP 端口；native RPC 端口不会收到 HTTP 健康探针。profile 发布前用 SHA 实际制品中的原 Http/ListenerProperties 类型绑定公共监听参数，校验 loopback、主/管理端口、TLS 模式及保护文件引用/权限。此处只检验原模型与文件元数据，不创建 SSL context，不证明证书握手、实际 listener 或完整 engine readiness。

文件级模型测试默认关闭。显式选择 `IsolatedOAuthProvisioningIT#packagedGatewayListenerModelsBindWithoutStartingApplications`，传入 `-Dxingyuan.isolated.gateway-profile.enabled=true`、repository-root 和同一私有 profile-inventory-file，可用实际 Biz/MCP JAR 检验原类型；测试中的文件为路径绑定用例数据，不能用于启动服务或当作真实 PEM/TLS 证据。


§12.55 的身份前置增加 `secretFiles.provisioner-password`，由隔离 prepare 使用原保护写入流程生成，CLI-002 在调用原 owner 前检查长度/权限；不要把口令贴进命令或聊天。原身份服务建立第三个独立目录身份及两租户 membership，私有 registry 新字段 `provisioningSubject` 与 admin/viewer 均不同，登录用 `subjects` 仍只有两个键。显式 resume 核对原三份 password hash 和六条 membership，不自动 rotate。

CLI-003 原 Directory 检查须确认三身份 ACTIVE、六条 membership，再发布 membershipCount=6 的保护凭据。身份桥不能单独证明 native 角色准备完成；后续仍须原 JA UserCrudFacade 用户、≤10分钟精确 ASSIGN_ROLE 策略、原 AssignmentFacade、disable及无业务授权证明。旧任务缺新字段/保护引用时会拒绝，不隐式改旧任务数据。当前仅受控源码测试，尚未启动实际 owner、DB或服务。


原JA fixture 新增 `IsolatedAuthorizationProvisioningIT#provisionNativeActors` 子阶段。前置是同任务 identity/原platform授权、`native-metadata.json` 与 `native-catalog-review.json` 均已真实提交，以及三身份Directory/六membership可用。它以原 `UserCrudFacade` 为每tenant创建viewer/provisioner，保存私有 `native-actors.json` 的实际JA userId映射，AS identity sub不会充当JA userId。所有前置和空目标检查先完成，再写RUNNING，随后才创建四条JA用户行；不直接写用户表，不赋业务角色、BusinessAccess或active root。

以下是后续真实准备的命令合同，当前未执行；须已有原控制面及已冻结保护配置：

```bash
XINGYUAN_ISOLATED_INTEGRATION=true ./mvnw -o \
  -pl egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am \
  '-Dtest=IsolatedAuthorizationProvisioningIT#provisionNativeActors' \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dxingyuan.isolated.repository-root="<repo>" \
  -Dxingyuan.isolated.manifest-file="<task-root>/intent.json" \
  -Dxingyuan.isolated.receipt-file="<task-root>/evidence/db-rbac.json" test
```

预期得到同run/manifest/OID/registry及catalog-review SHA绑定的COMMITTED私有actor凭据。provisioner通过原assignment/business/activation读取证明无业务授权，原admin platform snapshot不变。显式 `-Dxingyuan.isolated.resume=true` 对COMMITTED只读复查、不再create；RUNNING/未知提交不自动重放。后续viewer合法获得角色后，actor复查仍只强制provisioner无授权，不清空viewer状态。此actor凭据不等于native授权或CLI-004完成；父流程还会执行临时策略、原assign/disable、激活和投影门禁。


`IsolatedAuthorizationProvisioningIT#provisionNativeRoleAssignments` 继续使用同一JA准备fixture，前置是原platform授权、native metadata/catalog-review/actors保护凭据真实COMMITTED。该子阶段先核Directory、membership、JA ID与空角色/策略状态，保存带16个确切policy intent code的RUNNING凭据，再经原业务访问、角色、资源和字段owner准备两tenant×四app×admin/viewer角色。已有平台MANUAL BusinessAccess的ID、状态、有效期、reason/ticket保留，原owner按CAS推进version；不会用native列表替换掉原平台范围。

每次分配使用独立原管理策略：USER provisioner JA ID、CUSTOM_USER确切目标、单个新role root、仅ASSIGN_ROLE、LOW/PASSWORD、reason/ticket必填、禁止继承、至多10分钟。原DIRECT assignment有效8小时、非platformAdministrator；不把最低PASSWORD基线当作浏览器登录或STRONG/step-up证明。原save/policy完整回读相符后才assign，成功或分配异常后均尝试禁用已经确认属于本intent的策略；CAS漂移不覆盖，cleanup错误保留在原异常中。只有实际assignment读回引用同policy ID且原policy再次读为DISABLED，才写子阶段COMMITTED。

以下仍是后续运行阶段的命令合同，当前未执行：

```bash
XINGYUAN_ISOLATED_INTEGRATION=true ./mvnw -o \
  -pl egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin -am \
  '-Dtest=IsolatedAuthorizationProvisioningIT#provisionNativeRoleAssignments' \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dxingyuan.isolated.repository-root="<repo>" \
  -Dxingyuan.isolated.manifest-file="<task-root>/intent.json" \
  -Dxingyuan.isolated.receipt-file="<task-root>/evidence/db-rbac.json" test
```

预期私有 `native-role-assignments.json` 标明 `scope=NATIVE_ROLE_ASSIGNMENTS_ONLY`，包含16个原role/version、assignment/authVersion、已禁用policy及时间/资源树SHA证明。COMMITTED显式resume只读核对，不再replace/create/assign/disable；RUNNING/未知提交不得盲重放。此子阶段保留原active roots，未执行native角色激活，不代表最终USER snapshot、字段WRITE运行验证或公开CLI-004 native已完成。真实执行/投影/故障验收仍未运行，Agent运行未验证决定保持不变。


CLI004 native父流程现已接入 `IsolatedAuthorizationProvisioningIT#provisionNativeAuthorization`：按metadata→catalog review→actors→role assignments→显式activation顺序执行。前置仍为platform/control已成功和正式default/admin天枢USER token-index；调用已有命令时带 `--resume`，无需为尚未启动的消费者提前准备完整token矩阵。

最后阶段调用原RoleActivationFacade，管理员保留四平台根并选择四native根，viewer选择四native根，provisioner始终零业务授权。逐一核对原Redis USER snapshot的当前identity/authVersion/policyVersion、有效期、角色/assignment及审核permission/resource/field tuple（字段受原definition cap限制）。只有两tenant×两user四份证明通过，`authorization-native-owner.json`和公共`authorization-native.json`才标COMMITTED；任何RUNNING/未知提交均停止，COMMITTED显式resume只读重验。私有proof把原`sha256:64hex`摘要规范为64hex，原公开snapshot保持原算法。

新增源代码与受控回归不代表真实平台已经打通；真实PG/Redis/Directory、字段WRITE与失败恢复仍待Step18，Agent运行未验证的决定保持。

离线 native 构建证明使用私有源码工具入口，仍不增加九个公开命令。该入口从当前已提交 HEAD 导出独立副本，复用原生成器完成两次全七族 generate/check，再执行原七族 integration-test；不复制未提交的 source-projects 工作，也不覆盖原 `.generated`。三种 Open 的源码及生成内容按路径、模式和 SHA 保留，四种 native 必须实际产出 Boot JAR、原安全测试和构造注入测试的非零执行/零跳过结果；Agent 运行延后也不能免除其构建。例：

```bash
# 使用新的真实 UUID；settings 必须是仓库 .runtime 下审核过的 0600 文件。
export XINGYUAN_NATIVE_BUILD_ROOT="$PWD/.runtime/xingyuan-plan-execution/step17-native-build-<UUID>"
XINGYUAN_ISOLATED_INTEGRATION=true python3 -I scripts/unified-xingyuan/lib/isolated_integration.py \
  native-build-provenance "$PWD" "$XINGYUAN_NATIVE_BUILD_ROOT/native-build-provenance.json" \
  "<repo>/.runtime/<reviewed-maven-settings.xml>"
```

预期只产生新隔离构建目录、私有日志和 `native-build-provenance.json`，不创建数据库或启动应用。首次失败目录保留，不能将失败日志或旧 JAR补成成功证明；新执行使用新 UUID。完成后将该证明原字节放入新任务 `policies/native-build-provenance.json` 并在 manifest.policyFiles 固定 SHA，四个 native artifact 引用使用证明内真实 path/SHA；其余 buildId/catalog 仍必须来自原登记事实。CLI005在读取设施和启动前复核证明、原安全报告、当前源码差异、七族生成内容、Open和四个实际JAR；缺输入退出4，变化退出3。私有 observe-native-build 只将复核过的构建事实带入本次 verify attempt，不能单独满足24项运行验收。

缓存证明的生产/消费端使用同一份冻结 Agent 延后策略：仅允许排除 native-agent，且保留其源码、六层边界、安全测试与真实生成构建义务。Light GraphQL局部源码另以真实openid输入核两alias各自AUTHORIZATION_DENIED、实际原get/create计数零变化及合法别名请求恢复；这仍是待Step18运行的源码，未打开浏览器或发起真实HTTP。


Step17 的任务 MCP 使用原玉衡 Resource URI 和正式 PUBLIC/USER 输入，不增加 Client、OAuth Grant 或 scope。full 在六项 Biz 策略/完整性拒绝及恢复后，通过原玉衡 owner 给同一任务 group 增加唯一 `xingyuan-it-<runUUID>` server、`scopes` resource 和显式 `it-mcp-security` policy。resource 的内部 URI 是 `egon://xingyuan-it-<runUUID>/scopes`，原 `LOCAL_OPERATION` driver 调用实际已发现的 `GET /api/v1/yuheng/admin/scopes`；内部 URI 不能填写 HTTPS 业务地址。policy 固定业务 scope `yuheng:read`，细权限另由原 capability PDP 判定。MCP 私有 profile 必须显式覆盖已有 key：

```properties
egon.cola.platform.tianquan.jianshen.system-code=yuheng-admin
```

该值仍须与当前 registry 唯一 ACTIVE 玉衡 Resource 的 `rbacApplicationCode` 一致。原默认 `mock-backend` 不能作为此任务授权的证据。

在 `policies/mcp-authorization.json` 明确审核下列唯一数据范围，并把文件 SHA 放进原 manifest.policyFiles；文件模式0600，目录0700。`<runUUID>` 替换为本次完整小写 UUID，`<runHex>` 为去连字符的小写 UUID，`<RUNHEX>` 为对应大写值；businessCode 必须取原平台 catalog 中玉衡应用所属实际业务编码。

```json
{
  "schemaVersion": 1,
  "runId": "<runUUID>",
  "processId": "mcp",
  "tenantKeys": ["default", "tenant-b"],
  "applicationCode": "yuheng-admin",
  "businessCode": "<actual-business-code>",
  "permissionCode": "mcp:xingyuan-it-<runUUID>:resource:scopes:read",
  "resourceCode": "yuheng-admin.mcp-task-<runHex>",
  "roleCode": "XINGYUAN_MCP_<RUNHEX>_ADMIN",
  "userKey": "admin",
  "approved": true
}
```

只有该精确文件可进入原鉴神准备入口：PermissionCatalogService 建立 PENDING_VALIDATION 后显式 ACTIVE；机械 catalog command 的 permissions 为空、suggestedPermissionCode 为空，再用原 mapping owner 绑定新 API resource。原 Role/ResourceGrant/Assignment/Activation owner 只给两租户原 admin 增加任务角色，保留旧 active roles，viewer 不授。实际角色/策略版本、Outbox 增量和 Redis projection 都须回读。写前保存不可覆盖 RUNNING intent；未知结果保留现场，不能用 `--resume` 盲建另一条权限或再次授权。私有桥接由 full 固定调用，不增加第十条公开 CLI 命令。

MCP 验收必须实际调用 `resources/read`；initialize 只用于建立会话。每租户正例/恢复各进入原玉衡业务一次，缺细权限、缺 scope、错 audience 和裸请求均业务零调用。审计按实际 trace 读取 PG、Stream 与签名 STORED receipt：成功转发经过 MCP 和玉衡两次独立验签，两个 USED 必须同一原 proof；403 为一个 USED 和一个 DENY。签名事件协议没有 receiver 字段，因此不补造来源；对成功事件同时检查两个已知 spool 的候选文件，20 个实际事件对应28个需确认已消失的候选路径。这不等于删除28个事件，也不改变生产审计协议。

Light/Web 的 test-only Query.user 装饰器把原已封装安全 snapshot 的 callback 交给一个有界 worker，原 delegate 仅执行一次。私有计数只有 `differentThread`、`matchedContext`、`finallyRestored`、`failure`；正例两个 alias 必须2/2/2/0，scope 拒绝仍须换线程/恢复且原业务零调用。上下文只在内存比较，不导出 principal、tenant、token、proof、方法参数或业务结果。异步 HTTP、MCP/PG/Redis/browser 和外部模型运行仍未执行。

## 8. Step19 审计后的实际状态（2026-10-16）

本手册第2、5、6节的所有运行命令**仍未由本轮执行**。Step18的真实隔离联调经用户于2026-10-16明确批准跳过，原因是本机缺少其前置输入：经人工核对的`intent-manifest.json`、`postgres`/`redis`凭据文件、九角色secret文件，以及两租户真实owner提交的issuer/registration/clientID registry导出。第7节的隔离harness源码已随Step17（e99c004d7）提交完毕，其契约回归`bash scripts/regression/test-isolated-integration-contract.sh`在2026-10-16实测exit0（6组PASS、0 failure/error，全程以stub替换psql/createdb/redis-cli/nginx/java/node/npm/mvnw，零外部设施）。

因此当前本机状态：

- 未创建任何新数据库；`egon_identity_local`、`egon_rbac3_unified_identity_local`、`egon_gateway_local`、`egon_tianshu_local`及其数据未被触碰。
- 未启动应用、PostgreSQL、Redis、Edge、nginx、浏览器或Docker；未导入任何身份/OAuth/租户数据。
- 未写入任何run-scoped secret、token index、cert或receipt；`target/`下的`xingyuan-integration-*`目录来自Step17受控contract测试，不是真实运行产物，也不含有效凭据。
- 没有半provision、UNKNOWN_COMMIT或待恢复的durable intent遗留。

要执行真实验收，操作者需先按第1节备齐私密凭据文件与registry配置，再按Step18的九CLI顺序运行；在那之前，第5节的24项LIVE与第6节的owned stop保留比对一律记为`Runtime unverified`，不能用源码门禁、fixture页面、类型检查或CLI签发的令牌替代。逐项审计结论见[实施Plan §12.128](../egon/plan/2026-10-11-12-03-xingyuan-native-full-integration-plan.md)。
