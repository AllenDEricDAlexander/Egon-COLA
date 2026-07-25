# GWS-03 Gateway Engine Core 与 HTTP 数据面 Spec

状态：已实现，待用户验收

父文档：`2026-07-24-gateway-component-design.md`

索引：`2026-07-25-gateway-child-spec-index.md`

依赖：GWS-01

主模块：

- `egon-cola-component-gateway-core`
- `egon-cola-component-gateway-engine`

## 1. 目标

实现 Reactor Netty/Netty 之上的自研 Gateway Engine Core，承载：

- PUBLIC/INTERNAL HTTP Listener；
- HTTP 请求规范化与 Route Match；
- 自有 `GatewayExchange` 与 Filter Chain；
- HTTP Provider 调用；
- 统一响应、异常、生命周期和 LKG Rule Runtime 接口。

本 Spec 不实现 RPC Listener、DDC Provider Directory、具体限流算法、鉴权 Provider、
Admin、Starter 或 Kafka 事件细节。

## 2. 入口场景

### 2.1 PUBLIC HTTP

```text
Internet / App / Third Party
  → trusted deployment entry
  → Engine PUBLIC Listener
  → Route + externalAccessible + security + governance
  → HTTP or RPC Provider
```

### 2.2 INTERNAL HTTP

```text
Internal Service
  → Engine INTERNAL Listener
  → Route + configured security + governance
  → HTTP or RPC Provider
```

PUBLIC/INTERNAL 是 Listener 固有属性，不读取客户端自报 Header。

## 3. 网络运行时

### 3.1 Reactor Netty

- 使用 Reactor Netty `HttpServer` 和 `HttpClient`；
- Engine 不依赖 Spring Cloud Gateway；
- Netty EventLoop 上禁止数据库、Redis、DDC、Kafka 同步等待和阻塞式 JSON 处理；
- 必须对阻塞扩展点进行独立有界 Scheduler 隔离；
- EventLoop、Worker、连接池和业务隔离线程使用不同命名前缀。

### 3.2 Listener

```yaml
egon:
  cola:
    component:
      gateway:
        engine:
          http:
            public:
              enabled: true
              port: 8080
            internal:
              enabled: true
              port: 8081
```

规则：

1. 两个 Listener 使用不同端口；
2. 可在仅内部环境关闭 PUBLIC；
3. 两个 Listener 共享同一 Rule Runtime 和 Gateway Core；
4. Listener 启动成功不代表 Engine Ready；
5. 端口冲突或 Listener 启动失败使 Engine 进入 FAILED；
6. TLS 可以由受信任部署入口终止；Engine 直连 TLS 属于运行配置，证书管理平台不在
   本项目范围；
7. Spring Boot WebFlux/Actuator 使用独立 Management Port，只承载固定健康端点和
   GWS-06 节点状态接口；PUBLIC/INTERNAL 数据面仍是 Engine 自己创建的 Reactor
   Netty `HttpServer`，不经过 Spring `DispatcherHandler`、RouterFunction 或
   Spring Cloud Gateway。

### 3.3 协议范围

首期支持：

- HTTP/1.1 请求；
- 常用 Method；
- JSON、Form、文本和二进制透传；
- Chunked/流式上游响应透传；
- Keep-Alive 和连接池。

首期不支持：

- WebSocket；
- GraphQL 专用协议；
- 任意文件落盘；
- 无限请求体；
- 客户端通过 Header 切换 Access Zone。

## 4. Gateway Exchange

### 4.1 请求模型

```text
DefaultGatewayRequest
├── requestId
├── traceContext
├── accessZone
├── scheme
├── authority
├── rawPath
├── normalizedPath
├── method
├── query
├── headers
├── cookies
├── remoteAddress
└── body
```

### 4.2 Body

Body 有两种消费方式：

1. `AggregatedBody`：参数绑定、JSON 转换、签名校验；
2. `StreamingBody`：无需转换的 HTTP 透明代理。

规则：

- 同一个 Body 只能有一个主消费者；
- 需要多个 Filter 读取时由 Body Cache 明确启用；
- 默认聚合上限 2 MiB，可按 Route 下调或在受控范围上调；
- 超限返回 `REQUEST_BODY_TOO_LARGE`；
- Body Buffer 必须在成功、异常、取消时释放；
- 不允许把完整 Body 写入普通日志。

### 4.3 Context

Context 使用类型化 Slot：

```text
TraceContext
RouteContext
SecurityContext
GovernanceContext
ProviderContext
InvocationContext
ObservationContext
```

Filter 只能修改自己负责的 Context。不能使用公开可变 Map 作为跨阶段共享状态。

## 5. HTTP 规范化

进入路由前执行：

1. Method 大写规范化；
2. Host 小写、移除默认端口；
3. Path 只做一次百分号解码；
4. 拒绝非法 UTF-8、NUL、路径穿越和重复编码；
5. 默认拒绝编码后的 `/` 与 `\`；
6. Query 保留重复 Key 和原始顺序；
7. Header Name 大小写不敏感；
8. 移除 Hop-by-Hop Header；
9. 限制 Header 数量、单值和总字节数。

规范化结果用于匹配，原始值只用于受控转发和诊断。

## 6. Route 模型

### 6.1 Runtime Route

```text
RuntimeRoute
├── routeId
├── operationId
├── gatewayGroupId
├── accessZones
├── hostPredicate
├── methodPredicate
├── pathPattern
├── externalAccessible
├── upstream
├── policyRefs
├── priority
└── metadata
```

Runtime Route 来自 GWS-06 的不可变 Rule Snapshot。Engine 不接受运行期临时
Controller 注册 Route。

### 6.2 Path Pattern

首期支持：

- 精确：`/orders/current`；
- 变量：`/orders/{orderId}`；
- 尾部通配：`/assets/**`。

不支持任意正则表达式作为 Path。避免 ReDoS 和不同实现语义漂移。

### 6.3 匹配顺序

候选 Route 按以下顺序确定：

1. Host 精确优先于 Host 通配；
2. Method 精确匹配；
3. Path 精确优先于变量，变量优先于尾部通配；
4. 静态 Segment 更多者优先；
5. 显式 `priority` 更高者优先；
6. 仍无法唯一确定时视为发布冲突，不允许依靠列表顺序运行。

Admin 发布校验应消除歧义；Engine 仍保留防御性冲突检测。

### 6.4 Route Index

Route Compiler 生成不可变索引：

```text
HostIndex
└── MethodIndex
    └── PathTrie
        └── RuntimeRoute
```

- 请求热路径不扫描全部 Route；
- 编译在流量线程外完成；
- 激活通过单个原子引用切换；
- 旧索引在无请求引用后释放。

## 7. Filter Chain

固定逻辑阶段：

| 顺序 | 阶段 | 说明 |
|---|---|---|
| 100 | Request Identity | requestId、Trace |
| 200 | Access Zone | Listener 绑定 PUBLIC/INTERNAL |
| 300 | Request Guard | Method、Header、Body、路径保护 |
| 400 | Route Match | 查找 Runtime Route；预检按目标 Method 匹配 |
| 500 | Exposure | `externalAccessible` |
| 600 | CORS | 预检和响应 Header |
| 700 | Authentication | GWS-08 |
| 800 | Authorization | GWS-08 |
| 900 | Rate/Concurrency | GWS-07 |
| 1000 | Binding | 参数与 Schema |
| 1100 | Provider Selection | GWS-05 |
| 1200 | Invocation | HTTP/RPC Upstream |
| 1300 | Response Mapping | 透明或统一响应 |
| 1400 | Observation | 日志、指标、Kafka |

扩展 Filter：

- 必须声明阶段和 Order；
- 不得插入 Exposure 之前绕过外部访问检查；
- 不得在 Observation 之后改变业务结果；
- 短路结果必须使用统一 Gateway Error；
- 同 Order 冲突时启动失败，不依赖 Bean 顺序。

## 8. Gateway Executor

网络层只调用：

```java
public interface GatewayExecutor {
    Publisher<GatewayResponse> execute(GatewayExchange exchange);
}
```

职责：

1. 捕获同步与异步异常；
2. 执行 Filter Chain；
3. 保证 Observation/资源释放阶段最终执行；
4. 在客户端取消时取消上游请求；
5. 防止同一 Exchange 重复执行；
6. 统一映射结果到网络响应。

`Publisher` 最终可以具体化为 Reactor `Mono`，但 Domain Filter 不直接依赖
Reactor Netty Request/Response。

## 9. HTTP 参数绑定

支持来源：

- Path Variable；
- Query；
- Header；
- Cookie；
- Form；
- JSON Body；
- 原始 Body。

绑定依据：

- Starter 上报 Schema；
- Route 的参数映射；
- 明确的默认值和必填规则。

规则：

1. 缺失、类型失败、约束失败分别返回稳定错误；
2. 不根据客户端传入类名反射加载 Java Class；
3. JSON 只绑定到受控 Schema/树模型；
4. 同一目标字段多来源冲突必须有固定优先级或发布失败；
5. 参数名缺失不能通过编译器调试名猜测；
6. 敏感参数在日志与事件中只保留存在性或掩码。

## 10. HTTP Upstream Adapter

### 10.1 调用

`HttpUpstreamAdapter` 接收已经选择的 Provider Instance：

```text
service identity
+ instance host/port/secure
+ operation HTTP contract
+ bound request
+ deadline
```

Adapter 不负责服务发现和负载均衡。

### 10.2 URL 构造

- Scheme 来自 DDC protocol/secure；
- Host/Port 来自有效 Provider Lease；
- Path 来自 Operation Contract 和受控模板变量；
- 禁止 Route 提供任意绝对 URL 绕过 Provider Directory；
- 禁止 Host Header 覆盖实际连接目标；
- Query 和 Path 编码只执行一次。

### 10.3 Header

转发白名单/黑名单：

- 移除 Hop-by-Hop Header；
- 移除客户端伪造的内部身份 Header；
- 写入 Trace、Request ID 和可信身份 Header；
- `X-Forwarded-*` 只由 Engine 重建；
- 不转发 DDC/Admin Secret；
- Provider 响应中的危险 Header 按规则过滤。

### 10.4 连接池

- 连接池按协议、目标实例和 TLS 配置隔离；
- Provider Lease 摘除后停止新借用并 Drain 旧连接；
- 设置连接、响应和空闲超时；
- 连接池耗尽使用稳定 `UPSTREAM_CAPACITY_EXHAUSTED`；
- 不为每个请求新建 HttpClient。

## 11. 响应模式

Route 明确选择：

### 11.1 Transparent

- 保留受允许的上游 Status、Header 和 Body；
- 适合文件、流式和已有稳定 HTTP API；
- 网关自身错误仍使用 Gateway Error。

### 11.2 Wrapped

- 把成功或上游错误映射为统一 `ResultDto` 风格响应；
- 只适合已定义响应 Schema 的接口；
- 发布时校验 Mapper；
- 不能对未知二进制响应做 JSON 包装。

两种模式不得通过运行期内容猜测。

## 12. CORS

- CORS 是 Route/Group Policy；
- 预检请求在鉴权前处理，但必须先完成 Host/Route 范围识别和 Exposure 检查；
- `OPTIONS` 预检使用 `Access-Control-Request-Method` 查找目标 Route，不要求额外配置
  一条业务 `OPTIONS` Operation；
- Origin、Method、Header 使用明确列表；
- `allowCredentials=true` 时不能使用 `*` Origin；
- PUBLIC 与 INTERNAL 可配置不同规则；
- CORS 失败不调用 Provider。

## 13. 生命周期与资源释放

启动：

1. 创建基础资源；
2. 启动 Listener，但保持 Readiness false；
3. 注册 DDC Config Client；
4. 加载 DDC Rule/LKG；
5. 编译 Route、初始化必要连接资源；
6. 所有必要能力 Ready 后对外 Ready。

停止：

1. Readiness false；
2. 注销 RPC Gateway Slot 和 DDC Config Client 租约，避免成为新发布 Target；
3. 停止接受新请求；
4. 保持 Provider Directory 足够时间以完成在途请求；
5. 等待在途请求到 Drain Timeout；
6. 取消剩余请求；
7. 关闭 Provider 订阅、连接池、Listener、Scheduler；
8. 进入 STOPPED。

异常退出不承诺完成注销，DDC TTL 负责最终摘除。

## 14. 配置

关键配置：

```text
engine.http.public.enabled
engine.http.public.port
engine.http.internal.enabled
engine.http.internal.port
engine.http.max-initial-line-bytes
engine.http.max-header-bytes
engine.http.max-header-count
engine.http.default-max-body-bytes
engine.http.connection-idle-timeout
engine.http.drain-timeout
engine.http.upstream.max-connections
engine.http.upstream.pending-acquire-max-count
```

全部必须有边界校验。非法配置在启动时失败，不在首个请求时暴露。

## 15. 错误映射

| 场景 | Error Code | HTTP |
|---|---|---|
| 非法请求行/Header/Path | `GATEWAY_REQUEST_INVALID` | 400 |
| Body 超限 | `GATEWAY_REQUEST_BODY_TOO_LARGE` | 413 |
| 无 Route | `GATEWAY_ROUTE_NOT_FOUND` | 404 |
| Route 冲突 | `GATEWAY_ROUTE_AMBIGUOUS` | 500 |
| PUBLIC 调内部接口 | `GATEWAY_EXTERNAL_NOT_ACCESSIBLE` | 404 |
| 无 Provider | `GATEWAY_PROVIDER_UNAVAILABLE` | 503 |
| 连接失败 | `GATEWAY_UPSTREAM_CONNECT_FAILED` | 502 |
| 上游超时 | `GATEWAY_UPSTREAM_TIMEOUT` | 504 |
| Engine 无规则 | `GATEWAY_RULE_NOT_READY` | 503 |
| 未分类异常 | `GATEWAY_INTERNAL_ERROR` | 500 |

外部不可访问返回 404，避免泄漏内部接口存在性。

## 16. 测试设计

### 16.1 Core 单元测试

- Path 规范化、重复编码和穿越拒绝；
- Route 优先级与冲突；
- Path Trie 大量 Route 查找；
- Filter 顺序、短路和异常；
- Body 单消费与释放；
- Transparent/Wrapped 响应。

### 16.2 Engine 组件测试

- PUBLIC/INTERNAL 两个真实端口；
- Access Zone Header 伪造无效；
- Reactor Netty 真实 HTTP Provider；
- Keep-Alive、连接池、Chunked Response；
- 客户端取消传播；
- Listener 启停和 Drain；
- Rule 原子替换期间并发请求只看到完整旧版或新版。

### 16.3 安全与资源测试

- Header Bomb、Body 超限、慢请求；
- 非法 Host、Path、编码；
- Buffer 泄漏检测；
- EventLoop 阻塞检测；
- Provider 连接耗尽。

## 17. 验收标准

1. Engine 不依赖 Spring Cloud Gateway；
2. PUBLIC/INTERNAL 必须由独立 Listener 确定；
3. Route Match 不扫描全部 Route；
4. Route 冲突在发布/编译阶段失败；
5. EventLoop 无阻塞 DDC/DB/Kafka 调用；
6. Body 超限和资源释放可验证；
7. HTTP Provider 地址只来自 GWS-05 的 Provider Instance；
8. Rule 切换在单节点原子完成；
9. Drain 后不接受新请求且有界结束在途请求；
10. HTTP 错误符合 GWS-01 统一错误模型。

## 18. 本轮审核项

1. 认可双 HTTP Listener、同一 Engine Core；
2. 认可首期 Path Pattern 和固定匹配优先级；
3. 认可 2 MiB 默认聚合 Body 上限及流式透传分离；
4. 认可固定 Filter 阶段和不可绕过 Exposure；
5. 认可 Transparent/Wrapped 两种显式响应模式；
6. 认可 HTTP Upstream 只能使用 DDC Provider Instance 地址；
7. 认可 Engine 启停、Readiness 和 Drain 顺序。
