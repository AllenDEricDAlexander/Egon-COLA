# Four-Platform Direct Local Run Design

> 状态：基于已批准的统一身份平台架构实施
> 日期：2026-08-03
> 范围：IdP、RBAC3、Gateway、DDC 的本机直接打包、启动和全链路验收

## 1. 目标

在保留真实 PostgreSQL、Redis、统一 SSO、多租户授权、DDC 注册发现和 Gateway 路由的前提下，使开发者完成一次安全准备后，可以：

1. 用 Maven 生成固定名称的可执行 Jar；
2. 从仓库根目录直接执行 `java -jar ...-exec.jar` 启动各后端，不再手工 `source` 环境文件；
3. 在四个 Admin Web 目录直接执行 `npm run dev`，无需额外指定代理端口；
4. 通过一份可重复执行的验收矩阵验证四个平台的核心管理链路及跨平台链路；
5. 密码、私钥、服务 JWT 和访问令牌继续只保存在 Git 忽略且权限受限的运行目录。

## 2. 已确认问题

- 五个后端进程（Gateway 包含 Admin 与 Engine）都能产出 `*-exec.jar`，但 Jar 只会读取进程环境；`target/local-unified-platform/env/*.env` 不会被 Spring Boot 自动加载。
- 当前 `.env` 使用 Shell 转义格式，不能作为稳定的 Java Properties 契约直接复用。
- RBAC3、Gateway、DDC Admin Web 的 Vite 默认代理仍指向旧端口 `8080`、`8080`、`18080`，只有统一启动脚本覆盖环境变量时才正确。
- 现有深度验收覆盖 SSO、撤销、授权快照、DDC LKG、Gateway 发布和 MCP，但没有明确证明“脱离启动脚本、仅用 `java -jar`/`npm run dev`”的启动方式。

## 3. 方案

### 3.1 一次准备，随后直接运行

保留 `target/local-unified-platform` 作为唯一开发运行目录。准备流程继续负责：

- 检查 PostgreSQL/Redis；
- 创建缺失的具名数据库；
- 生成本机密码、RSA 密钥、服务凭据；
- 打包可执行 Jar；
- 生成 Shell 使用的 `.env` 与 Spring Boot 使用的同名 `.properties`。

每个后端 `application.yml` 使用可选的 `spring.config.import` 自动读取对应 `.properties`：

```yaml
spring:
  profiles:
    default: local
  config:
    import: optional:file:${UNIFIED_PLATFORM_RUNTIME_DIR:target/local-unified-platform}/env/<service>.properties
```

环境变量和命令行参数仍拥有更高优先级，因此该机制只提供本机默认值，不改变部署环境覆盖能力。配置文件缺失或外部 PostgreSQL/Redis 不可用时，应用必须明确启动失败，不能静默退化成另一套数据存储。

### 3.2 安全边界

- 仓库不提交任何密码、私钥、JWT 或真实 Access Key。
- `.properties` 与 `.env` 一样写入 Git 忽略目录，目录权限 `0700`、文件权限 `0600`。
- `.properties` 只改变配置加载方式，不放宽 OAuth、CORS、RBAC3、DDC 签名或 Gateway 身份校验。
- 本方案只面向本机开发，不声明生产部署、容器、多机或高可用能力。

### 3.3 前端直接启动

四个 Vite 工程保留各自端口：

| Web | 端口 | 默认 `/api` 代理 |
|---|---:|---|
| IdP Admin Web | 18121 | `http://127.0.0.1:18120` |
| RBAC3 Admin Web | 18131 | `http://127.0.0.1:18130` |
| Gateway Admin Web | 18141 | `http://127.0.0.1:18140` |
| DDC Admin Web | 18152 | `http://127.0.0.1:18150` |

OAuth issuer、Client ID、Audience 和 Redirect URI 继续使用各前端已有的安全默认值。显式环境变量仍可覆盖代理与 OAuth 参数。

## 4. 启动顺序与命令契约

一次准备：

```bash
scripts/unified-platform/prepare-local-stack.sh
```

后端直接启动顺序固定为 DDC、IdP、RBAC3、Gateway Admin、Gateway Engine。Gateway 是一个平台但包含控制面与数据面两个 JVM：

```bash
java -jar egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/target/egon-cola-platform-dynamic-config-center-admin-exec.jar
java -jar egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/target/egon-cola-platform-idp-admin-exec.jar
java -jar egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/target/egon-cola-platform-rbac3-admin-exec.jar
java -jar egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/target/egon-cola-platform-gateway-admin-exec.jar
java -jar egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/target/egon-cola-platform-gateway-engine-exec.jar
```

准备流程只创建配置、依赖、数据库与构建物，不负责持久运行应用。首次空数据库仍由现有统一启动流程完成管理员和跨平台拓扑初始化；已初始化的本机数据可直接使用上述 Jar 命令重复启动。

## 5. 验收矩阵

### 5.1 独立启动验收

- 五个 Jar 在不 `source` 环境文件、不额外传 Spring 参数的情况下启动并通过 readiness；
- 四个 Web 仅执行 `npm run dev` 后根页面返回 200；
- 四个 Web 的 `/api` 代理使用正确后端端口；
- 四个 OAuth Client 完成 CORS、Authorization Code + PKCE 与 Token 交换。

### 5.2 平台核心链路

- IdP：登录、用户状态、OAuth Client、JWK、Token/Refresh/Replay/撤销、审计读取；
- RBAC3：租户映射、角色候选与激活、授权快照、撤权与原 Token 恢复、运行状态；
- DDC：业务域、环境、应用、命名空间、环境应用绑定、配置发布、服务注册、LKG；
- Gateway：应用目录、凭据、Gateway Group、草稿、校验、发布、非法发布保护、Provider 路由、HTTP 与 MCP；
- 跨平台：同一 SSO 多租户、Gateway 只做身份校验、下游独立 RBAC3 授权、双 Engine 会话/任务、Remote MCP 熔断恢复。

验收脚本必须输出脱敏的逐项结果；任何必需链路失败都返回非零退出码。

## 6. 错误处理

- 配置目录存在但具体服务 `.properties` 缺失时，准备验收失败并指出文件路径；
- 端口占用时 Vite 和 Spring Boot 必须失败，不自动漂移端口；
- 直接启动验收必须先确认目标进程确实由本次 `java -jar`/`npm run dev` 命令创建，不能复用旧 PID 冒充通过；
- 验收产生的临时业务数据使用唯一测试编码，并在可安全恢复时清理；安全撤销、LKG 和熔断场景必须恢复原状态；
- 不输出密码、私钥、服务 JWT 或 Access Token 内容。

## 7. 非目标

- 不把本机秘密写入 Git；
- 不用 H2、SQLite 或无密码 Redis 替换真实联调拓扑；
- 不把五个后端合并成一个进程；
- 不改变 Gateway 只做基础身份校验、下游自行授权的既定边界；
- 不把本机验证结果表述为生产环境或多机验证。
