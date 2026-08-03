# IdP/RBAC3 本地 DDC 服务注册设计

## 目标

本地统一身份平台启动后，DDC“服务注册目录”必须同时显示 `idp-admin` 和
`rbac3-admin`，展开后分别能看到处于 `ONLINE` 状态的 HTTP Provider 实例。

## 根因

Gateway 引擎和测试 Provider 已显式启用 DDC Registry，并由
`HttpProviderLeaseRuntime` 或对应 Gateway 生命周期负责注册、续约和注销。
IdP 本地配置明确关闭 DDC；RBAC3 的 local profile 也覆盖关闭 DDC Registry 和
HTTP Provider。启动脚本没有为两者生成 Registry 连接和 Provider 身份，因此当前
DDC 全局服务目录只有 Gateway 相关服务。

## 方案

复用现有 `egon-cola-platform-gateway-provider-runtime`，不新增注册协议或生命周期。
IdP 和 RBAC3 保持完整 DDC 动态配置客户端默认关闭，只为本地拓扑独立启用：

- `egon.cola.component.ddc.registry.enabled`：创建 DDC 服务注册客户端；
- `egon.cola.component.gateway.provider.http.enabled`：在 Web Server 就绪后注册
  `HTTP_PROVIDER`，定时续约，停机注销；
- DDC 物理身份使用 `bizCode=identity`、`env=local`，应用和服务名分别为
  `idp-admin`、`rbac3-admin`；namespace 继续只作为可见性范围，不进入物理
  `serviceId`；
- 实例 ID 分别为 `idp-local-1`、`rbac3-local-1`，地址为本机实际 HTTP 端口
  `18120`、`18130`。

本地脚本生成 DDC endpoint、签名凭据和 Redis Registry 连接，但不在日志或文档中
输出凭据值。生产 profile 的既有行为不变。

## 错误与恢复

DDC 在 IdP/RBAC3 之前启动。注册成功后每 10 秒续约，租约为 30 秒；心跳失败时
沿用 `HttpProviderLeaseRuntime` 的重新注册逻辑，停机时主动注销，异常退出由 TTL
清理。服务注册不依赖完整 DDC 配置订阅，因此不会因为缺少应用动态配置而阻塞登录。

## 验收

1. 本地配置生成测试证明 IdP/RBAC3 都获得独立的 Registry 与 HTTP Provider 开关、
   正确的身份、端口和 DDC 连接参数。
2. RBAC3 local profile 测试证明 Registry/Provider 可由本地变量启用，同时完整 DDC
   与 Gateway Reporting 仍默认关闭。
3. IdP/RBAC3 定向测试和 Maven 打包成功。
4. 重启 DDC、IdP、RBAC3 后，DDC 服务目录出现两个应用及在线实例。

