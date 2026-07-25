# GWS-08 Gateway 安全扩展 Spec

状态：草案，等待审核

父文档：`2026-07-24-gateway-component-design.md`

索引：`2026-07-25-gateway-child-spec-index.md`

依赖：GWS-01、GWS-03、GWS-04、GWS-06

## 1. 目标

本 Spec 定义 Gateway 数据面的信任边界、外部暴露约束、身份处理流程以及认证、授权
扩展点。首期不实现下游业务权限系统，但 Gateway 不能因此成为无安全边界的透明代理。

目标包括：

1. PUBLIC 与 INTERNAL 流量使用不可伪造的物理入口标记；
2. `externalAccessible=false` 的接口不能由公网入口调用；
3. 认证与授权能力可由后续业务安全组件接入；
4. 未配置具体鉴权实现时仍执行入口隔离、Header 清洗、规则校验和安全审计；
5. HTTP 与 RPC 使用统一安全上下文和失败语义；
6. 安全扩展不得阻塞 Netty EventLoop 或泄露凭据。

## 2. 范围与非目标

### 2.1 本期范围

- PUBLIC/INTERNAL Listener 信任边界；
- 接口外部可访问性控制；
- Credential 提取 SPI；
- Authentication、Authorization SPI；
- Principal、AuthContext 和安全决策模型；
- 可信身份向 HTTP/RPC Provider 的受控透传；
- 管理规则对安全 Provider 的引用校验；
- 安全失败错误、日志、指标和测试。

### 2.2 明确不做

- 不实现下游权限系统；
- 不内置 JWT、OAuth2、OIDC、SAML、Shiro 或 Spring Security 业务规则；
- 不托管登录、发 Token、刷新 Token 或用户会话；
- 不从普通请求 Header 接受 PUBLIC/INTERNAL 来源声明；
- 不把 Gateway Admin 登录鉴权纳入本数据面 Spec；
- 不在 Starter 中执行鉴权；
- 不在 Gateway 保存业务用户密码、私钥或长期令牌；
- 不允许安全插件直接操作 Netty Response 或绕过统一错误模型。

## 3. 信任边界

### 3.1 Listener 决定 Access Zone

Engine 按 GWS-03 启动两个独立 Listener：

| Listener | `AccessZone` | 典型来源 | 默认约束 |
|---|---|---|---|
| Public HTTP | `PUBLIC` | 外部负载均衡、Ingress、客户端 | 只允许外部可访问接口 |
| Internal HTTP | `INTERNAL` | 内网服务、测试应用、运维探针 | 可访问内部接口，仍受规则约束 |
| Internal RPC | `INTERNAL` | Egon RPC Consumer | 仅内网，不对公网监听 |

`AccessZone` 在连接被 Listener 接受时写入 `GatewayContext`，后续 Filter 只读。
下列 Header/Metadata 即使存在也不能改变它：

```text
X-Gateway-Access-Zone
X-Internal-Request
X-Forwarded-Internal
gateway-access-zone
```

### 3.2 可信代理

PUBLIC Listener 可以读取标准转发地址头，但只有连接对端属于配置的可信代理 CIDR 时才
接受 `Forwarded` 或 `X-Forwarded-For`。否则以 TCP 对端地址为准，并记录一次低基数
安全指标。

首期不根据源 IP 自动把 PUBLIC 请求升级为 INTERNAL。若部署需要内部来源，必须走
Internal Listener。

### 3.3 TLS 边界

- Engine 支持 Listener 直接 TLS 或由上游基础设施终止 TLS；
- 若依赖上游终止 TLS，是否可信由部署配置决定，不由客户端 Header 决定；
- 本期不在 Admin 动态下发证书私钥；
- mTLS 身份提取可以作为后续 `CredentialExtractor`，首期不内置 CA 管理。

## 4. 外部可访问性

### 4.1 字段语义

每个 Operation 定义包含：

```text
externalAccessible: boolean
```

规则：

1. 默认值为 `false`；
2. `false` 表示只允许 INTERNAL Listener；
3. `true` 表示该接口有资格被 PUBLIC 路由，但不会自动创建或启用 Route；
4. PUBLIC Route 引用 `false` Operation 时 Admin 发布失败；
5. 旧规则或异常规则漏过发布校验时，Engine 运行时仍拒绝；
6. INTERNAL 请求可以访问 `true` 或 `false` 接口，但仍执行配置的认证和授权策略；
7. 字段变化必须生成新的接口定义/治理发布审计，不允许静默生效。

### 4.2 执行顺序

外部可访问性在路由候选确定后、读取大 Body 和调用安全插件前执行：

```text
Listener 标记 AccessZone
→ 路由匹配
→ externalAccessible 检查
→ Credential 提取
→ Authentication
→ Authorization
→ 后续治理与 Provider 调用
```

PUBLIC 访问内部接口返回 `404` 或统一的 `GATEWAY_ROUTE_NOT_FOUND`，默认不向外部暴露
“接口存在但仅限内网”。内部审计记录真实拒绝原因
`GATEWAY_EXTERNAL_ACCESS_DENIED`。

## 5. 安全领域模型

### 5.1 Credential

```text
GatewayCredential
├── type
├── tokenReference
├── attributes
└── sensitive = true
```

- `tokenReference` 是仅在当前请求内存中使用的凭据，不得进入普通日志、指标、Kafka
  调用事件或 Admin；
- Attribute 必须有固定 Schema 和大小上限，不能成为任意无界 Map；
- 多个 Extractor 可以产生多个不同类型 Credential；
- 相同类型出现歧义时按规则决定拒绝或使用明确优先级，不能随机选择。

### 5.2 Principal

```text
GatewayPrincipal
├── principalId
├── principalType
├── tenantId
├── displayName
├── authenticated
└── attributes
```

约束：

- 匿名主体使用显式 `ANONYMOUS`，不使用 null；
- `principalId`、`tenantId` 和 Attribute 长度受限；
- Principal 只能由认证 Provider 产生，不能直接信任入站身份 Header；
- Display Name 仅用于诊断，授权不得依赖可变显示名称。

### 5.3 AuthContext 与决策

`GatewayAuthContext` 包含：

- Access Zone；
- Operation、Route 和 Policy 身份；
- 请求方法、规范化路径或 RPC Full Method；
- Credential 摘要，不含可记录的原始 Secret；
- Principal；
- 客户端网络上下文；
- Trace/Request ID；
- Deadline 与当前规则版本。

统一决策：

```text
ALLOW
DENY
ABSTAIN
ERROR
```

`ABSTAIN` 不等于允许；其最终行为由 Policy 的组合模式决定。

## 6. 扩展 SPI

### 6.1 Credential Extractor

```java
public interface GatewayCredentialExtractor {
    String type();
    Mono<CredentialExtractionResult> extract(
        GatewayExchange exchange,
        SecurityPolicy policy
    );
}
```

职责：

- 从 Header、Cookie、mTLS Session 或 RPC Metadata 提取凭据；
- 检查格式和大小；
- 不验证业务身份；
- 提取结束后标记需要从上游请求中移除的字段。

### 6.2 Authentication Provider

```java
public interface GatewayAuthenticationProvider {
    String providerId();
    Set<String> supportedCredentialTypes();
    Mono<AuthenticationDecision> authenticate(
        GatewayAuthContext context,
        GatewayCredential credential
    );
}
```

### 6.3 Authorization Provider

```java
public interface GatewayAuthorizationProvider {
    String providerId();
    Mono<AuthorizationDecision> authorize(GatewayAuthContext context);
}
```

### 6.4 Identity Mapper

```java
public interface GatewayIdentityMapper {
    String mapperId();
    TrustedIdentityHeaders map(GatewayAuthContext context);
}
```

Identity Mapper 只输出规则白名单内的可信字段，不能透传原始 Credential。

### 6.5 执行约束

- SPI 使用异步返回类型，禁止在 Netty EventLoop 上执行阻塞 I/O；
- 必须接入统一 Deadline、取消和超时；
- 需要调用阻塞遗留系统的 Adapter 必须使用独立有界线程池，并受并发隔离；
- Provider 不得修改 Route、Operation 或 Access Zone；
- Provider ID 在 Engine 启动时唯一；
- 插件初始化失败时，引用它的规则不能生效；
- SPI 实现由 Spring Bean 发现，但 Core 只依赖接口。

此处使用责任链模式：安全阶段存在明确顺序、多个可插拔实现和短路失败语义。直接把
全部判断写入路由 Handler 会造成协议重复并阻碍后续安全系统接入。

## 7. 安全策略

规则快照中的 `SecurityPolicy` 至少包含：

```text
policyId
authenticationMode = NONE | OPTIONAL | REQUIRED
credentialExtractorIds[]
authenticationProviderIds[]
authorizationProviderIds[]
decisionMode = ALL_ALLOW | ANY_ALLOW
identityMapperId
providerTimeoutMs
failureMode = FAIL_CLOSED
```

约束：

1. PUBLIC Operation 默认不隐式附加业务鉴权，但仍执行外部暴露检查；
2. `REQUIRED` 时没有 Credential 直接拒绝；
3. `OPTIONAL` 时没有 Credential 使用匿名 Principal，有非法 Credential 时仍拒绝；
4. `NONE` 不调用业务认证 Provider；
5. 首期只允许 `FAIL_CLOSED`，不支持认证系统异常时放行；
6. `ALL_ALLOW` 要求所有 Authorization Provider 返回 ALLOW；
7. `ANY_ALLOW` 至少一个 ALLOW，且 ERROR/DENY 的优先规则必须在发布时固定；
8. 所有 Provider、Extractor、Mapper 引用必须在每个目标 Engine 上存在；
9. Engine Capabilities 不满足时发布失败，而不是等请求到达后才发现。

## 8. Header 与 Metadata 清洗

### 8.1 入站清洗

在执行认证前移除或隔离所有可能伪造内部身份的字段，包括：

```text
X-Gateway-Principal-Id
X-Gateway-Tenant-Id
X-Gateway-Authenticated
X-Gateway-Auth-Provider
X-Gateway-Access-Zone
gateway-principal-id
gateway-tenant-id
```

真实列表由 Engine 固定保留命名空间与规则附加黑名单共同组成。规则不能从固定保留
名单中删除字段。

### 8.2 出站身份

只有 Identity Mapper 可以写入可信身份字段：

- HTTP 使用固定 `X-Egon-Gateway-*` 命名空间；
- RPC 使用 Egon RPC 允许的 Metadata Key；
- 写入前覆盖而不是追加同名入站值；
- 未认证请求写显式匿名标志或不写，按 Mapper 契约固定；
- 不透传 Access Token、Session Cookie 或原始 Authorization Header，除非后续专门
  的 Credential Relay 扩展经过独立审核。

### 8.3 Hop-by-Hop 与敏感字段

安全清洗叠加 GWS-03 的 Hop-by-Hop Header 规则。日志、错误、Kafka 事件和治理诊断
只记录 Credential 类型、Provider ID 和结果，不记录值。

## 9. HTTP 与 RPC 一致性

| 行为 | HTTP | RPC |
|---|---|---|
| Access Zone | Listener 固定 | Internal RPC Listener 固定 |
| 凭据来源 | Header/Cookie/TLS | Metadata/TLS |
| 未认证 | 统一错误响应 | 映射 `UNAUTHENTICATED` |
| 无权限 | 统一错误响应 | 映射 `PERMISSION_DENIED` |
| 超时 | Gateway 安全超时 | 同时受 gRPC Deadline 限制 |
| 身份透传 | 白名单 Header | 白名单 Metadata |

HTTP→RPC 转发时只执行一次 Gateway 安全链，再由 RPC Identity Mapper 生成可信
Metadata，不把 HTTP Cookie 或 Header 原样复制到 RPC。

## 10. 错误与失败语义

内部错误码：

| Code | 外部行为 | 含义 |
|---|---|---|
| `GATEWAY_CREDENTIAL_INVALID` | 401 / `UNAUTHENTICATED` | 凭据格式非法 |
| `GATEWAY_AUTHENTICATION_REQUIRED` | 401 / `UNAUTHENTICATED` | 缺少必需凭据 |
| `GATEWAY_AUTHENTICATION_FAILED` | 401 / `UNAUTHENTICATED` | 身份认证失败 |
| `GATEWAY_AUTHORIZATION_DENIED` | 403 / `PERMISSION_DENIED` | 授权拒绝 |
| `GATEWAY_SECURITY_PROVIDER_TIMEOUT` | 503 / `UNAVAILABLE` | 安全 Provider 超时 |
| `GATEWAY_SECURITY_PROVIDER_ERROR` | 503 / `UNAVAILABLE` | 安全 Provider 异常 |
| `GATEWAY_EXTERNAL_ACCESS_DENIED` | 对 PUBLIC 隐藏为 404 | 外部访问内部接口 |
| `GATEWAY_IDENTITY_MAPPING_FAILED` | 500 / `INTERNAL` | 可信身份生成失败 |

响应不能包含 Provider 堆栈、Token 片段、租户敏感属性或内部接口存在性。

## 11. 发布与运行期校验

Admin 发布前校验：

- PUBLIC Route 只能引用 `externalAccessible=true` Operation；
- Provider/Extractor/Mapper ID 非空且 Schema 合法；
- 超时在允许范围内；
- 决策模式与 Provider 数量一致；
- 禁止首期不支持的 Fail Open；
- 规则引用的 Engine Capability 在全部目标节点可用。

Engine 编译快照时再次校验：

- SPI Bean 存在且唯一；
- Credential 类型兼容；
- HTTP/RPC Mapper 支持目标协议；
- 安全 Filter 顺序没有被插件改变。

新快照失败时保留 LKG。运行期间某个外部安全 Provider 不可用时按请求 Fail Closed，
不撤销规则、不自动切换为匿名。

## 12. 配置与 Secret

- 规则只保存 `providerId` 和非敏感参数，不保存 Client Secret；
- Secret 使用部署环境或后续统一 Secret Component 注入；
- Admin 页面不读取 Engine Secret；
- Secret 轮换不得要求重发接口定义；
- 安全 Provider 配置变更应有独立审计；
- DEBUG 日志也不得输出 Credential、Cookie、Authorization 或完整 Principal
  Attribute。

## 13. 可观测性

低基数指标：

```text
gateway_security_decision_total{stage,result,providerId}
gateway_security_duration_seconds{stage,providerId}
gateway_external_access_denied_total{listener}
gateway_identity_sanitized_total{protocol}
gateway_security_provider_inflight{providerId}
```

禁止把 `principalId`、`tenantId`、Token、Path 原值作为指标 Label。

审计事件包含：

- 时间、Trace ID、Operation/Route/Rule 版本；
- Access Zone；
- Credential 类型；
- Provider ID；
- 决策与标准化原因；
- Principal 类型和不可逆摘要（可选）。

## 14. 测试设计

### 14.1 单元测试

1. Listener 固化 Access Zone，伪造 Header 无效；
2. `externalAccessible` 默认 false；
3. PUBLIC 访问内部 Operation 被拒绝，INTERNAL 可继续；
4. Header/Metadata 保留名单清洗；
5. REQUIRED、OPTIONAL、NONE 三种模式；
6. ALL_ALLOW、ANY_ALLOW 组合与短路；
7. Provider 缺失、重复、超时、异常全部 Fail Closed；
8. Identity Mapper 只能写白名单字段；
9. 错误和日志不包含 Credential；
10. 取消信号能终止异步安全调用。

### 14.2 集成测试

1. PUBLIC/Internal HTTP Listener 对同一内部接口产生不同结果；
2. HTTP 与 RPC 错误映射一致；
3. HTTP→RPC 只透传 Mapper 生成的可信 Metadata；
4. 规则引用不存在 Provider 时 Engine 拒绝 ACK；
5. 模拟阻塞安全 Adapter 时 EventLoop 不被占用；
6. 可信代理与非可信代理的客户端地址解析；
7. LKG 下安全策略不被异常新版本覆盖。

### 14.3 安全测试

- 身份 Header 注入；
- 重复 Authorization/Header Smuggling；
- 超长 Credential；
- CRLF Header 注入；
- Cookie/Metadata 泄露；
- PUBLIC 探测内部接口；
- 安全 Provider 超时洪泛与隔离。

## 15. 验收标准

1. PUBLIC/INTERNAL 由 Listener 决定且不能由请求伪造；
2. `externalAccessible=false` 同时受 Admin 发布和 Engine 运行期双重保护；
3. 没有具体下游权限系统时，安全边界和扩展链仍完整工作；
4. HTTP、RPC 与 HTTP→RPC 使用同一 Principal/Decision 语义；
5. 入站身份字段被清洗，出站身份只由可信 Mapper 产生；
6. 所有业务安全异常默认 Fail Closed；
7. 安全扩展不阻塞 EventLoop；
8. Credential 不出现在日志、指标、Kafka 或 Admin 数据中；
9. 规则引用缺失 Capability 时不能发布或生效。

## 16. 本轮审核项

1. 认可双 Listener 作为不可伪造的 PUBLIC/INTERNAL 边界；
2. 认可 `externalAccessible` 默认 false 且只表示“有资格被外部路由”；
3. 认可首期只提供安全 SPI，不实现具体下游权限系统；
4. 认可认证/授权异常统一 Fail Closed；
5. 认可清洗入站身份并由 Identity Mapper 生成可信身份；
6. 认可 PUBLIC 探测内部接口时对外隐藏为未找到。
