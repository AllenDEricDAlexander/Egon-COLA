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
