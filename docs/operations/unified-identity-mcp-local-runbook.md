# 统一身份与 Gateway MCP 本地联调 Runbook

本文说明如何在开发机上启动、验证和停止 IdP、RBAC3、DDC、Gateway 与完整 MCP 联调拓扑。脚本复用本机 PostgreSQL 和 Redis，不使用容器，不删除已有数据库、密钥或验收证据。

## 拓扑与端口

| 组件 | 地址 | 用途 |
|---|---|---|
| IdP Admin Web | `http://127.0.0.1:18121` | 统一登录入口和 IdP 管理界面 |
| RBAC3 Admin Web | `http://127.0.0.1:18131` | RBAC3 管理界面 |
| Gateway Admin Web | `http://127.0.0.1:18141` | 统一 SSO 登录后的 Gateway 管理界面 |
| DDC Admin Web | `http://127.0.0.1:18152` | DDC 管理界面 |
| IdP | `http://127.0.0.1:18120` | OAuth/OIDC、用户和会话 |
| RBAC3 Admin | `http://127.0.0.1:18130` | 下游授权快照与角色激活 |
| Gateway Admin | `http://127.0.0.1:18140` | Gateway/MCP 控制面 |
| DDC Admin | `http://127.0.0.1:18150` | 配置、注册发现和发布通知 |
| Gateway MCP A | `http://127.0.0.1:18180/mcp/unified-local` | Stable、RC 和 Legacy 公共入口 |
| Gateway MCP B | `http://127.0.0.1:18184/mcp/unified-local` | 双节点会话与任务恢复验证入口 |
| Mock Backend | `http://127.0.0.1:18160` | 统一身份 HTTP 验证夹具 |
| MCP Provider | `http://127.0.0.1:18161` | 本地 Operation 调用夹具 |
| Remote MCP | `http://127.0.0.1:18151` | Stable/RC 远端联邦夹具 |

Gateway 只执行用户存在性、JWT 合法性等基础身份校验；业务系统继续自行解析 JWT，并从 RBAC3 拉取与缓存当前用户的授权快照。

## 前置条件

- Java 21、`curl`、`jq`、`openssl`、`psql`、`createdb`、`redis-cli`、Node.js/npm。
- 本机 PostgreSQL 可通过 `127.0.0.1:5432` 访问；默认用户为 `postgres`，首次运行时密码必须通过受保护的 `UNIFIED_IDENTITY_POSTGRES_PASSWORD_FILE` 或 `UNIFIED_IDENTITY_POSTGRES_PASSWORD` 显式提供，后续可复用权限为 `0600` 的 runtime secret。脚本不会猜测或修改账号密码。
- 本机 Redis 可通过 `127.0.0.1:6379` 访问。脚本优先从 `/opt/homebrew/etc/redis.conf` 读取 `requirepass`。
- 若本机配置不同，通过 `UNIFIED_IDENTITY_POSTGRES_*`、`UNIFIED_IDENTITY_REDIS_*` 环境变量或相应密码文件覆盖；不要把密码写入仓库。

当前联调链路不依赖 Kafka。启动脚本只校验并复用用户已有的 PostgreSQL/Redis，不负责启动或停止它们。

## 启动

在仓库根目录执行：

```bash
scripts/unified-platform/start-local-stack.sh
```

首次执行会完成 Maven 打包、Admin Web 构建、数据库初始化、IdP 用户引导、RBAC3 多租户角色初始化、DDC 应用与注册初始化，以及 Gateway HTTP/MCP 统一发布。本地 MCP Tool 由 Provider 的 `@GatewayInterfaceGroup` 和 `@GatewayOperation` 注解投影，脚本只协调 Server、Remote MCP 和其他控制面能力，不创建本地 Tool 或 disabled Route 锚点。

需要验证交付给开发者的原生命令时，先执行一次：

```bash
scripts/unified-platform/prepare-local-stack.sh
```

准备流程会初始化同一完整拓扑并停止所有受管进程。随后从仓库根目录分别运行五个后端；JAR 会自动读取 `target/local-unified-platform/env/*.properties`：

```bash
java -jar egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/target/egon-cola-platform-dynamic-config-center-admin-exec.jar
java -jar egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/target/egon-cola-platform-idp-admin-exec.jar
java -jar egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/target/egon-cola-platform-rbac3-admin-exec.jar
java -jar egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/target/egon-cola-platform-gateway-admin-exec.jar
java -jar egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/target/egon-cola-platform-gateway-engine-exec.jar
```

四个 Admin Web 则分别进入各自目录执行同一条无参数命令，默认代理已固定为对应的 `18120`、`18130`、`18140`、`18150` 后端端口：

```bash
npm run dev
```

准备流程会在四个 Admin Web 目录生成受管的 `.env.local`，把真实数值型默认租户 ID 注入 `VITE_DEFAULT_TENANT_ID`；因此无参数启动不会再把租户代码 `default` 错当成租户 ID。

直接启动顺序为 DDC、IdP、RBAC3、Gateway Admin、Gateway Engine；等待后端 Readiness 为 `200` 后再启动 Web。首次数据库初始化必须经过准备流程，不能只启动空库上的 JAR。

只在已确认可执行 JAR 和 Web 构建均为最新时，才可跳过构建：

```bash
UNIFIED_PLATFORM_SKIP_BUILD=true scripts/unified-platform/start-local-stack.sh
```

## 状态与日志

```bash
scripts/unified-platform/status-local-stack.sh
```

状态必须显示 13 个受管进程均为 `running`，健康码均为 `200`。运行时目录为 `target/local-unified-platform/`：

- PID：`target/local-unified-platform/pids/`
- 日志：`target/local-unified-platform/logs/`
- 脱敏验收证据：`target/local-unified-platform/evidence/verification-summary.json`
- QA 密钥：`target/local-unified-platform/secrets/`，目录权限为 `0700`，文件权限为 `0600`

脚本和日志不会输出访问令牌、服务凭据或明文密码。

## 登录 Admin Web

打开 `http://127.0.0.1:18141`，通过统一 IdP SSO 登录：

- 用户名：`alice`
- 密码文件：`target/local-unified-platform/secrets/idp-admin.password`

在本机终端读取密码：

```bash
sed -n '1p' target/local-unified-platform/secrets/idp-admin.password
```

不要把密码复制到文档、Issue、聊天记录或 Git 提交中。默认租户与 `tenant-b` 已初始化，用于多租户隔离验证。

## 深度验收

```bash
scripts/unified-platform/verify-local-stack.sh
```

验收覆盖：

- IdP SSO、tokenVersion 撤销和重新登录；
- RBAC3 角色激活、授权撤销传播及原 JWT 下的权限恢复；
- DDC 注册、配置中断时 LKG 连续服务和恢复；
- 注解托管的 MCP Operation 不依赖 Route，并在两个 Engine 上进入同一发布；
- Stable、RC、Legacy SSE，以及 Tools、Resources、Templates、Prompts、Completion、Apps、订阅；
- Engine A 创建会话/任务、Engine B 读取会话/任务；
- 本地 Operation、远端 MCP 联邦、熔断开启与恢复；
- PostgreSQL 持久任务和 Redis 跨节点会话/事件流。

验收会短暂中断并恢复 DDC、Remote MCP 和 RBAC3 权限；不会创建、删除或恢复用于 MCP 占位的草稿路由。`trap` 会在成功或失败时尽力恢复现场。若终端被强制杀死，重新执行启动脚本即可协调完整拓扑。

运行 MCP 官方 conformance：

```bash
egon-cola-platforms/egon-cola-platform-gateway/deployment/scripts/run-mcp-conformance.sh \
  http://127.0.0.1:18151/conformance/stable \
  http://127.0.0.1:18151/conformance/rc \
  target/local-unified-platform/evidence/mcp-conformance
```

脚本使用同一 Remote MCP 进程内隔离的
`/conformance/stable` 与 `/conformance/rc` 场景服务器，分别运行固定版本的
Stable Active Suite 和 RC Draft Suite。它们只提供官方 CLI 约定的诊断能力；
Gateway 实际远端联邦仍使用 `/remote/stable` 与 `/remote/rc`，两类夹具互不混用。
验收结果写入
`target/local-unified-platform/evidence/mcp-conformance/{stable,rc}/`。

## 停止

```bash
scripts/unified-platform/stop-local-stack.sh
```

停止脚本只处理 PID 文件记录且仍存活的受管进程，不停止本机 PostgreSQL/Redis，也不删除数据库、密钥、日志或证据。需要重新验证时再次执行启动脚本。

## 常见排查

- 某组件 `health=000`：查看 `target/local-unified-platform/logs/<component>.log`，然后重新执行启动脚本。
- IdP 登录失败：确认用户名是 `alice`，密码来自当前运行时目录的限制文件；不要使用旧运行时目录中的副本。
- 授权更新短暂未生效：本地 Gateway Engine 授权缓存 TTL 为 1 秒，先等待状态轮询；持续失败时检查 RBAC3 与两个 Engine 日志。
- Remote MCP 调用失败：确认 `mcp-remote` 健康；深度验收会主动触发熔断，恢复后等待约 4 秒。
- 发布失败：检查 Gateway Admin 日志和草稿校验响应；无效发布不得替换当前 LKG。
