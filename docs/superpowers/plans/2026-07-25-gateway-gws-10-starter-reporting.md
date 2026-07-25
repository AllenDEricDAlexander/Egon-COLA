# GWS-10 Gateway Starter 接口定义上报实施计划

> 对应 Spec：
> `docs/superpowers/specs/2026-07-25-gateway-starter-interface-reporting-design.md`

## 目标

交付 Provider 侧接口定义发现、规范化、指纹、HMAC 上报与 Admin 接收闭环。Starter
只处理“接口定义”，不注册 Provider、不拦截请求、不生成调用事件、不发送 Kafka。

## Task 1：公共上报契约与稳定身份

涉及：

- `egon-cola-component-gateway-contract`
- `GatewayDefinitionIdentity`
- `GatewayInterfaceDefinitionReport`
- HTTP/RPC、三级目录、Operation Definition、结果 DTO
- `GatewayInterfaceGroup`、`GatewayOperation` 注解

步骤：

1. 先写序列化、稳定排序、默认 `externalAccessible=false` 测试；
2. 定义 `v1` 契约和大小有界的 Schema/Descriptor 字段；
3. 实现 Canonical Fingerprint 与 Definition Set ID；
4. 验证扫描顺序不影响指纹，时间、Report ID 不参与指纹。

## Task 2：HTTP Controller 发现

涉及：

- `gateway-starter` HTTP scanner；
- Spring MVC/WebFlux 已注册 `RequestMappingHandlerMapping`；
- Jackson Schema 中间模型；
- Jakarta Validation 与参数位置。

步骤：

1. 用真实 Spring MVC 测试 Controller 固定类级/方法级、多映射基线；
2. 从最终 Handler Mapping 读取 Method/Path/Consumes/Produces；
3. 每个 Controller 映射为一个 Interface Group；
4. 展开每个 Method/Path 组合为独立 Operation；
5. 排除 Actuator、错误 Handler、Starter 自身和框架参数；
6. SSE/Streaming 标记 `UNSUPPORTED`，不隐藏定义；
7. `externalAccessible` 未注解时保持 false。

## Task 3：Egon RPC Contract 发现

涉及：

- 可选依赖 `egon-cola-component-rpc-starter`；
- `RpcContractCatalog`；
- Protobuf `FileDescriptorSet`。

步骤：

1. 使用 Catalog 测试替身固化一个 Contract 对应一个 Interface Group；
2. 只读取已验证 Snapshot，不重复扫描 Provider Bean；
3. 生成 Unary Operation、Descriptor SHA、稳定 RPC Operation Key；
4. 默认内部接口；
5. Streaming 在本地校验阶段拒绝。

## Task 4：Starter 生命周期与 HMAC 客户端

涉及：

- `GatewayReportingProperties`；
- Spring Boot AutoConfiguration；
- `RestClient`；
- 有界单线程上报 Worker；
- HMAC Signer、重试策略与状态 Bean；
- `AutoConfiguration.imports`。

步骤：

1. 先测试 canonical request、Body SHA、Nonce、时间戳和签名；
2. 在 `ApplicationReadyEvent` 后构建一次不可变 Payload；
3. 同一 Report 重试复用 Report ID 和 Payload；
4. 仅网络错误、429、5xx 指数退避；4xx 停止；
5. 默认失败不阻断业务应用，`fail-fast=true` 才抛出；
6. Worker 有界、应用关闭时取消，不占用 WebFlux EventLoop；
7. 暴露最近状态与 `GatewayDefinitionIdentity`，不落盘 Secret。

## Task 5：Admin HMAC、Nonce 与 Definition Set 入库

涉及：

- `gateway-admin/interfaces/reporting`；
- Credential 解密与 Scope 校验；
- `gateway_hmac_nonce` 原子占用；
- Definition Set、三级目录、Operation/Definition 入库；
- 报告查询与幂等结果。

步骤：

1. 先测试成功、过期、Body 篡改、Nonce 重放、Scope 越权；
2. 使用独立 `/api/v1/gateway/openapi/interface-definitions` Filter Chain，
   明确拒绝页面身份 Header；
3. 事务内写 Definition Set、目录和不可变 Definition；
4. 同 Report ID/同 Payload 返回原结果，不同 Payload 返回冲突；
5. 相同 Build ID/不同 Fingerprint 拒绝；
6. STARTER 定义不能覆盖人工治理配置；
7. 缺失接口只统计差异，不自动 OFFLINE、不触发发布；
8. 审计接受与拒绝，禁止记录 Payload/Secret。

## Task 6：验证与提交

验证：

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-starter,:egon-cola-component-gateway-admin \
  -am test
```

附加检查：

- Starter 依赖中没有 Kafka、Provider Lease 实现或 Nacos/Dubbo；
- Admin 仍只有 GWS-09 的一份新 Flyway Migration；
- HMAC Secret、Authorization、Cookie 不进入日志/审计；
- `git diff --check`。
