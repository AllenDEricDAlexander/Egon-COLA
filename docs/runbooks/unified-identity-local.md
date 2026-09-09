# 统一身份平台本机联调手册

本手册用于开发阶段在宿主机运行 Tianquan-Shoubing、Tianquan-Jianshen、Tianshu、Yuheng 和模拟下游，验证统一 JWT 身份、多租户、Yuheng 基础身份校验及下游授权。人员只有
USER Access Token 和 Refresh Token；浏览器通过 Yuheng 的 HttpOnly JWT Cookie 跨管理 Web 复用身份，不建立服务端登录
Session。脚本不使用 Docker，不删除已有数据库，也不面向生产环境。

## 1. 前置条件

- JDK 21，以及仓库内可用的 `./mvnw`。
- 本机 PostgreSQL 和 Redis 已启动。
- `curl`、`jq`、`openssl`、`psql`、`createdb`、`redis-cli`、`awk` 可执行。
- PostgreSQL 账号可以连接维护库并创建下列具名开发数据库。
- Redis 必须启用密码；脚本可从 `/opt/homebrew/etc/redis.conf` 的 `requirepass` 读取，或通过 `UNIFIED_IDENTITY_REDIS_PASSWORD_FILE` 指定只含密码的文件。

PostgreSQL 默认使用 `127.0.0.1:5432` 和用户 `postgres`，但脚本不会猜测或修改密码。首次准备必须通过受保护的密码文件或环境变量显式提供凭据；后续可复用运行目录中权限为 `0600` 的 `secrets/postgres.password`：

```bash
export UNIFIED_IDENTITY_POSTGRES_HOST=127.0.0.1
export UNIFIED_IDENTITY_POSTGRES_PORT=5432
export UNIFIED_IDENTITY_POSTGRES_USER=postgres
export UNIFIED_IDENTITY_POSTGRES_PASSWORD_FILE=/absolute/path/postgres.password
export UNIFIED_IDENTITY_REDIS_PASSWORD_FILE=/absolute/path/redis.password
```

默认开发数据库为：

- `egon_identity_local`
- `egon_rbac3_unified_identity_local`
- `egon_gateway_local`
- `egon_tianshu_local`

脚本只在数据库不存在时创建它们，不清库、不删除数据库、不修改其他数据库。

## 2. 准备、启动和验证

需要像最终交付一样逐个运行 JAR 和前端时，先在仓库根目录执行一次安全准备：

```bash
./scripts/unified-xingyuan/prepare-local-stack.sh
```

该命令会使用真实 PostgreSQL/Redis 构建 JAR、安装缺失的锁定版前端依赖，临时拉起并初始化 Tianquan-Shoubing USER JWT、Tianquan-Jianshen 双租户、Tianshu 和
Yuheng/MCP 拓扑，然后停止受管进程，为直接命令释放端口。生成的密钥和每个服务独立的 Spring Properties 位于
`target/local-unified-xingyuan/`，权限为 600，不进入 Git。

准备流程还会为四个 Admin Web 生成受管的 `.env.local`，写入 Tianquan-Jianshen 实际创建的数值型默认租户 ID。不要手工改成租户代码 `default`；授权接口的 `tenant_id` 契约是租户 ID。

准备完成后，分别在五个终端的仓库根目录运行；不需要 `source .env`，也不需要额外 JVM 参数：

```bash
java -jar egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin/target/egon-cola-tianshu-admin-exec.jar
```

```bash
java -jar egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/target/egon-cola-tianquan-shoubing-admin-exec.jar
```

```bash
java -jar egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/target/egon-cola-tianquan-jianshen-admin-exec.jar
```

```bash
java -jar egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/target/yuheng-admin-exec.jar
```

```bash
java -jar egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway/target/yuheng-biz-gateway-exec.jar
```

必须从仓库根目录执行这些命令，或者显式设置 `UNIFIED_XINGYUAN_RUNTIME_DIR` 为运行配置目录的绝对路径。首次创建数据库时不要跳过准备命令，因为 Tianquan-Jianshen 的本机身份绑定和四个平台的初始数据由准备流程建立。

以下旧命令继续用于一键编排或回归：

仅检查依赖、创建缺失的具名数据库、生成本机密钥并构建应用：

```bash
./scripts/unified-identity-local.sh prepare
```

构建并按 Tianshu → Tianquan-Shoubing → Tianquan-Jianshen → Yuheng Engine/Admin → 模拟下游的顺序启动，同时创建本机 OAuth Client、双租户 RBAC 拓扑和 Yuheng Release：

```bash
./scripts/unified-identity-local.sh start
```

执行统一身份后端验收：

```bash
./scripts/unified-identity-local.sh verify
```

执行包含四个 Admin Web、Yuheng 和 MCP 的完整平台验收：

```bash
./scripts/unified-xingyuan/verify-local-stack.sh
```

完整平台验收覆盖：

- 四个正在运行的 Vite 前端均加载真实默认租户 ID，且该租户会员解析为 `ACTIVE`。
- Yuheng 接受合法身份，但不做 RBAC 权限判断。
- 模拟下游在角色未激活时返回 403；同一 Access Token 在角色激活和缓存失效后返回 200。
- `default` 与 `tenant-b` USER Token 的 `tid` 不同、`sub` 相同；同一 Yuheng Cookie 模式可在多个 Admin Web 复用，不依赖
  `sid` 或服务端 Session。
- Refresh Token 稳定可重复使用且不轮换；Yuheng 只在 USER Access Token 缺失或确认过期时通过 RT 向 Tianquan-Shoubing 换取新的 5 分钟 AT。
- Yuheng Logout/强制退出只删除或失效 Tianquan-Shoubing 保存的 RT；已经签发的 AT 不会被反向撤销，最多继续有效到原 `exp`（默认五分钟）。RT
  被删除后，使用该 RT 刷新必须失败并重新登录。
- 角色激活集合按 Tianquan-Jianshen 用户授权事实保存；登录、Refresh、AT 过期、Yuheng 节点切换和跨 Admin Web 不会清空激活角色。未激活角色不进入权限上下文。
- Tianshu Provider、Yuheng Release、外部路由和下游直连均可用。
- `UnifiedIdentityTopologyIT`、`UnifiedIdentityRevocationIT`、`UnifiedIdentityTenantSwitchIT` 通过。

`verify` 会有意改变角色激活和 Token Version。再次执行完整验收前，建议先 `stop`，再 `start`，由启动流程恢复确定性角色基线。

## 3. 进程和端口

| 组件 | 地址 | 用途 |
| --- | --- | --- |
| Tianquan-Shoubing Admin | `http://127.0.0.1:18120` | OAuth 2.1、JWK、身份管理 |
| Tianquan-Shoubing Admin Web | `http://127.0.0.1:18121` | 登录入口和 Tianquan-Shoubing 管理台 |
| Tianquan-Jianshen Admin | `http://127.0.0.1:18130` | 租户映射、角色激活、授权快照 |
| Tianquan-Jianshen Admin Web | `http://127.0.0.1:18131` | Tianquan-Jianshen 管理台 |
| Yuheng Admin | `http://127.0.0.1:18140` | Yuheng 控制面 |
| Yuheng Admin Web | `http://127.0.0.1:18141` | Yuheng 管理台 |
| Tianshu Admin | `http://127.0.0.1:18150` | 动态配置和服务注册中心 |
| Tianshu Admin Web | `http://127.0.0.1:18152` | Tianshu 管理台 |
| Mock Backend | `http://127.0.0.1:18160` | Tianquan-Shoubing + Tianquan-Jianshen 下游样例 |
| Yuheng 外部端口 | `http://127.0.0.1:18180` | 对外路由入口 |
| Yuheng 内部端口 | `http://127.0.0.1:18181` | Yuheng 内部 Listener |
| Yuheng Engine 管理端口 | `http://127.0.0.1:18182` | Actuator/Readiness |

查看后端 PID 和 Readiness：

```bash
./scripts/unified-identity-local.sh status
```

## 4. 启动四个管理 Web

分别在四个终端运行：

```bash
cd egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web
npm run dev
```

```bash
cd egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web
npm run dev
```

```bash
cd egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web
npm run dev
```

```bash
cd egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web
npm run dev
```

本机管理员用户名为 `alice`。推荐准备命令生成的密码保存在 `target/local-unified-xingyuan/secrets/tianquan-shoubing-admin.password`，文件权限为
600；不要提交、复制到文档或写入前端存储。登录请求、Refresh、UserInfo、Logout 和管理 API 都经 Yuheng 对外提供；成功登录后，Yuheng
在浏览器 Cookie 中保存同一身份链的 USER AT/RT，进入另外三个管理 Web 时复用该 JWT Cookie，不需要 Tianquan-Shoubing SSO
Session、Authorization Code、PKCE 或前端 Token Store。切换租户时只重新建立该租户的 JWT Cookie，`sub` 保持不变而 `tid` 按租户变化。

## 5. 运行数据和日志

推荐准备命令的所有生成物位于 `target/local-unified-xingyuan/`；直接运行旧的 `unified-identity-local.sh` 时默认仍使用 `.runtime/unified-identity/`。两个目录均已被 Git 忽略：

- `logs/`：各后端日志。
- `pids/`：仅由本脚本管理的 PID。
- `env/`：本机进程环境文件，权限为 600。
- `secrets/`：密码、RSA Key 和受保护的短期 SERVICE/控制面验收凭据，权限为 600；浏览器 USER AT/RT 只在 Yuheng Cookie
  中使用，脚本不会把它们作为每个客户端的长期文件保存。
- `yuheng-biz-gateway-data/`：Yuheng Engine 本机状态。

常用诊断：

```bash
tail -n 200 .runtime/unified-identity/logs/tianquan-shoubing.log
tail -n 200 .runtime/unified-identity/logs/tianquan-jianshen.log
tail -n 200 .runtime/unified-identity/logs/yuheng-biz-gateway.log
tail -n 200 .runtime/unified-identity/logs/mock-backend.log
```

脚本把登录、角色激活、Yuheng 自动 Refresh 和 RT 失效响应保存在运行目录，并在失败信息中报告 HTTP 状态及安全响应体；不会打印
Access Token、Refresh Token 或密码。

## 6. 停止

```bash
./scripts/unified-identity-local.sh stop
```

该命令只停止 `pids/` 中由本脚本记录且仍存活的后端进程，保留数据库、Redis 数据、密钥、日志和构建产物。四个 Vite 进程需在各自终端用 `Ctrl-C` 停止。
