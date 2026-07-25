# Egon COLA Gateway Component

Gateway Component 是自研的 HTTP 与 RPC 业务网关平台，采用分层架构。Engine 与
Admin 是独立部署应用，Starter 与 HTTP Provider Runtime 是下游应用按需安装的两个
独立组件。

## 模块

- `egon-cola-component-gateway-contract`：稳定的跨进程契约；
- `egon-cola-component-gateway-core`：不依赖运行时框架的数据面模型与 SPI；
- `egon-cola-component-gateway-engine`：网关数据面可执行应用；
- `egon-cola-component-gateway-admin`：管理控制面可执行应用；
- `egon-cola-component-gateway-starter`：Provider 接口定义上报；
- `egon-cola-component-gateway-provider-runtime`：HTTP Provider DDC 租约运行时；
- `egon-cola-component-gateway-admin-web`：独立构建的 React 管理平台；
- `egon-cola-component-gateway-test`：真实 Provider 与端到端测试工程。

公共 Components BOM 只导出 Starter 与 Provider Runtime。Engine、Admin、
Contract、Core 和测试 Artifact 均为平台内部模块。

实现按照 `docs/superpowers/specs` 下已确认的 Gateway Spec 分阶段交付。
