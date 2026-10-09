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
