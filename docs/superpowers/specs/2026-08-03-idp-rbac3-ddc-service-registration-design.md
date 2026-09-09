# Tianquan-Shoubing/Tianquan-Jianshen 本地 Tianshu 服务注册设计

## 目标

本地统一身份平台启动后，Tianshu“服务注册目录”必须同时显示 `tianquan-shoubing-admin` 和
`tianquan-jianshen-admin`，展开后分别能看到处于 `ONLINE` 状态的 HTTP Provider 实例。

## 根因

Yuheng 引擎和测试 Provider 已显式启用 Tianshu Registry，并由
`HttpProviderLeaseRuntime` 或对应 Yuheng 生命周期负责注册、续约和注销。
Tianquan-Shoubing 本地配置明确关闭 Tianshu；Tianquan-Jianshen 的 local profile 也覆盖关闭 Tianshu Registry 和
HTTP Provider。启动脚本没有为两者生成 Registry 连接和 Provider 身份，因此当前
Tianshu 全局服务目录只有 Yuheng 相关服务。

## 方案

复用现有 `yuheng-provider-runtime`，不新增注册协议或生命周期。
Tianquan-Shoubing 和 Tianquan-Jianshen 已有发布安全门要求 Tianshu 配置客户端先达到 `READY`，所以本地拓扑启用：

- `egon.cola.component.tianshu.enabled`：建立配置客户端租约并满足既有发布安全门；
- `egon.cola.component.tianshu.registry.enabled`：创建 Tianshu 服务注册客户端；
- `egon.cola.component.yuheng.provider.http.enabled`：在 Web Server 就绪后注册
  `HTTP_PROVIDER`，定时续约，停机注销；
- Tianshu 物理身份使用 `bizCode=identity`、`env=local`，应用和服务名分别为
  `tianquan-shoubing-admin`、`tianquan-jianshen-admin`；namespace 继续只作为可见性范围，不进入物理
  `serviceId`；
- 实例 ID 分别为 `tianquan-shoubing-local-1`、`tianquan-jianshen-local-1`，地址为本机实际 HTTP 端口
  `18120`、`18130`。

Tianquan-Jianshen 的 Provider 租约状态观察只在 Yuheng Reporting 身份存在时装配；服务注册
本身不再强制开启 Yuheng Reporting，从而避免本地 Yuheng Admin 与 Tianquan-Jianshen 的启动
环。本地脚本生成 Tianshu endpoint、签名凭据和 Redis Registry 连接，但不在日志或
文档中输出凭据值。生产 profile 的既有行为不变。

## 错误与恢复

Tianshu 在 Tianquan-Shoubing/Tianquan-Jianshen 之前启动。注册成功后每 10 秒续约，租约为 30 秒；心跳失败时
沿用 `HttpProviderLeaseRuntime` 的重新注册逻辑，停机时主动注销，异常退出由 TTL
清理。Tianshu 配置客户端在服务发布前完成初始同步；没有已发布动态配置时使用应用本地
默认值，不阻塞登录。

## 验收

1. 本地配置生成测试证明 Tianquan-Shoubing/Tianquan-Jianshen 都获得独立的 Registry 与 HTTP Provider 开关、
   正确的身份、端口和 Tianshu 连接参数。
2. Tianquan-Jianshen local profile 测试证明 Registry/Provider 可由本地变量启用，同时完整 Tianshu
   与 Yuheng Reporting 仍默认关闭。
3. Tianquan-Shoubing/Tianquan-Jianshen 定向测试和 Maven 打包成功。
4. 重启 Tianshu、Tianquan-Shoubing、Tianquan-Jianshen 后，Tianshu 服务目录出现两个应用及在线实例。
