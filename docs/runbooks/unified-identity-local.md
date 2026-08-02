# 统一身份平台本机联调手册

本手册用于开发阶段在宿主机运行 IdP、RBAC3、DDC、Gateway 和模拟下游，验证统一 SSO、多租户、Gateway 基础身份校验及下游授权。脚本不使用 Docker，不删除已有数据库，也不面向生产环境。

## 1. 前置条件

- JDK 21，以及仓库内可用的 `./mvnw`。
- 本机 PostgreSQL 和 Redis 已启动。
- `curl`、`jq`、`openssl`、`psql`、`createdb`、`redis-cli`、`awk` 可执行。
- PostgreSQL 账号可以连接维护库并创建下列具名开发数据库。
- Redis 必须启用密码；脚本可从 `/opt/homebrew/etc/redis.conf` 的 `requirepass` 读取，或通过 `UNIFIED_IDENTITY_REDIS_PASSWORD_FILE` 指定只含密码的文件。

PostgreSQL 默认使用 `127.0.0.1:5432`、用户 `postgres`、密码 `postgres`。需要覆盖时使用：

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
- `egon_ddc_local`

脚本只在数据库不存在时创建它们，不清库、不删除数据库、不修改其他数据库。

## 2. 准备、启动和验证

仅检查依赖、创建缺失的具名数据库、生成本机密钥并构建应用：

```bash
./scripts/unified-identity-local.sh prepare
```

构建并按 DDC → IdP → RBAC3 → Gateway Engine/Admin → 模拟下游的顺序启动，同时创建本机 OAuth Client、双租户 RBAC 拓扑和 Gateway Release：

```bash
./scripts/unified-identity-local.sh start
```

执行完整验收：

```bash
./scripts/unified-identity-local.sh verify
```

验收覆盖：

- Gateway 接受合法身份，但不做 RBAC 权限判断。
- 模拟下游在角色未激活时返回 403；同一 Access Token 在角色激活和缓存失效后返回 200。
- `default` 与 `tenant-b` Token 的 `tid` 不同、SSO `sid` 相同。
- Refresh Token 单次轮换成功；旧 Cookie 重放后整个人员安全状态撤销，旧 Access Token 在 Gateway 返回 401。
- 禁用 `alice` 后旧 Access Token 在 Gateway 返回 401；断言后脚本只恢复自有开发夹具，并通过 IdP 启动回灌恢复 Redis 状态。
- 撤销后重新签发的两个租户 Token 可用。
- DDC Provider、Gateway Release、外部路由和下游直连均可用。
- `UnifiedIdentityTopologyIT`、`UnifiedIdentityRevocationIT`、`UnifiedIdentityTenantSwitchIT` 通过。

`verify` 会有意改变角色激活和 Token Version。再次执行完整验收前，建议先 `stop`，再 `start`，由启动流程恢复确定性角色基线。

## 3. 进程和端口

| 组件 | 地址 | 用途 |
| --- | --- | --- |
| IdP Admin | `http://127.0.0.1:18120` | OAuth 2.1、JWK、身份管理 |
| IdP Admin Web | `http://127.0.0.1:18121` | 登录入口和 IdP 管理台 |
| RBAC3 Admin | `http://127.0.0.1:18130` | 租户映射、角色激活、授权快照 |
| RBAC3 Admin Web | `http://127.0.0.1:18131` | RBAC3 管理台 |
| Gateway Admin | `http://127.0.0.1:18140` | Gateway 控制面 |
| Gateway Admin Web | `http://127.0.0.1:18141` | Gateway 管理台 |
| DDC Admin | `http://127.0.0.1:18150` | 动态配置和服务注册中心 |
| DDC Admin Web | `http://127.0.0.1:18151` | DDC 管理台 |
| Mock Backend | `http://127.0.0.1:18160` | IdP + RBAC3 下游样例 |
| Gateway 外部端口 | `http://127.0.0.1:18180` | 对外路由入口 |
| Gateway 内部端口 | `http://127.0.0.1:18181` | Gateway 内部 Listener |
| Gateway Engine 管理端口 | `http://127.0.0.1:18182` | Actuator/Readiness |

查看后端 PID 和 Readiness：

```bash
./scripts/unified-identity-local.sh status
```

## 4. 启动四个管理 Web

分别在四个终端运行：

```bash
cd egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web
npm run dev
```

```bash
cd egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web
RBAC3_ADMIN_PROXY=http://127.0.0.1:18130 npm run dev
```

```bash
cd egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web
GATEWAY_ADMIN_PROXY=http://127.0.0.1:18140 npm run dev
```

```bash
cd egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web
DDC_ADMIN_PROXY=http://127.0.0.1:18150 npm run dev
```

本机管理员用户名为 `alice`。首次准备时生成的密码保存在 `.runtime/unified-identity/secrets/idp-admin.password`，文件权限为 600；不要提交、复制到文档或写入前端存储。登录一次 IdP 后，进入另外三个管理 Web 会复用 IdP SSO Cookie，不再要求输入密码或 Token。切换租户会重新执行 Authorization Code + PKCE，但不会重新输入密码。

## 5. 运行数据和日志

所有生成物位于 `.runtime/unified-identity/`，该目录已被 Git 忽略：

- `logs/`：各后端日志。
- `pids/`：仅由本脚本管理的 PID。
- `env/`：本机进程环境文件，权限为 600。
- `secrets/`：密码、RSA Key、服务凭据和验收 Token，权限为 600。
- `gateway-engine-data/`：Gateway Engine 本机状态。

常用诊断：

```bash
tail -n 200 .runtime/unified-identity/logs/idp.log
tail -n 200 .runtime/unified-identity/logs/rbac3.log
tail -n 200 .runtime/unified-identity/logs/gateway-engine.log
tail -n 200 .runtime/unified-identity/logs/mock-backend.log
```

脚本把 OAuth、角色激活和 Refresh 失败响应保存在运行目录，并在失败信息中报告 HTTP 状态及安全响应体；不会打印 Access Token、Refresh Token 或密码。

## 6. 停止

```bash
./scripts/unified-identity-local.sh stop
```

该命令只停止 `pids/` 中由本脚本记录且仍存活的后端进程，保留数据库、Redis 数据、密钥、日志和构建产物。四个 Vite 进程需在各自终端用 `Ctrl-C` 停止。
