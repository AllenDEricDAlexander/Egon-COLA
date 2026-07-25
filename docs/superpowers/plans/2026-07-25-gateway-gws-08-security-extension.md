# Gateway GWS-08 安全扩展实现计划

**Goal:** 固化 Listener 信任边界，在不实现下游权限系统的前提下提供完整的凭据提取、
认证、授权、身份映射扩展链和 Fail Closed 语义。

**Architecture:** Core 定义无框架 Security Model/SPI；Engine 使用异步责任链执行
Extractor → Authentication → Authorization → Identity Mapper。Access Zone 只由
Listener 注入，入站保留身份字段在安全链前清洗。

## 设计模式判断

- Chain of Responsibility：安全阶段固定、可插拔并支持短路。
- Strategy：Provider/Extractor/Mapper 按稳定 ID 注册。
- Facade：`GatewaySecurityChain` 统一 HTTP、RPC 和 HTTP→RPC 决策。
- 不内置 JWT/OAuth 等具体实现，避免越过已确认的下游权限边界。

## Task 1: 安全模型和 SPI

- Credential、Principal、AuthContext、Decision、Policy。
- Credential 只在请求内存存在，属性有界，匿名 Principal 显式。
- SPI 使用 Reactive Streams Publisher，Core 不依赖 Reactor/Spring。

**Commit:** `feat(gateway): define security extension contracts`

## Task 2: Capability Registry 与规则编译

- Provider/Extractor/Mapper ID 唯一。
- 校验凭据类型、协议能力、Provider 引用与 FAIL_CLOSED。
- 缺失能力导致快照 Apply 失败并保留 LKG。

**Commit:** `feat(gateway): validate gateway security capabilities`

## Task 3: 异步安全责任链

- REQUIRED/OPTIONAL/NONE 和 ALL_ALLOW/ANY_ALLOW。
- Deadline、取消、超时与异常全部 Fail Closed。
- 生成统一 HTTP/gRPC 错误。

**Commit:** `feat(gateway): execute fail closed security chain`

## Task 4: 身份清洗与可信透传

- 固定保留 Header/Metadata 不允许被规则放宽。
- Identity Mapper 只写 `X-Egon-Gateway-*` 或 RPC 白名单。
- 不透传 Authorization、Cookie、Token 或 Access Zone 声明。

**Commit:** `feat(gateway): sanitize and map trusted identity`

## Task 5: 信任边界集成

- PUBLIC 内部接口对外隐藏为 404，内部审计保留真实原因。
- 可信代理 CIDR 才接受 Forwarded 地址，不升级 Access Zone。
- HTTP/RPC/HTTP→RPC 使用相同 Principal/Decision。

**Commit:** `feat(gateway): enforce listener trust boundaries`

## Task 6: 验收

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl :egon-cola-component-gateway-engine -am clean test
```

检查无具体业务鉴权实现，凭据不进入日志/Kafka/Admin，伪造身份 Header 不影响信任边界。
