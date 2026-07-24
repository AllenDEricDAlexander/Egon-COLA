# Egon-COLA Gateway 项目总览 Spec

状态：草案，等待审核

文档阶段：功能范围与技术路线对齐

项目定位：Egon-COLA Component 体系中的大型网关平台

## 1. 本文档要解决的问题

本文档只回答两个问题：

1. Gateway 项目最终需要具备哪些功能。
2. 这些功能采用什么总体技术路线实现。

本文档不是实施级详细设计，暂不展开：

- 数据库表结构和 Flyway 脚本；
- HTTP API、DTO 和错误码清单；
- 类、接口、包和配置项的最终命名；
- 路由发布状态机、消息体和协议字段；
- 单个页面的交互原型；
- 详细测试用例、性能指标和容量参数；
- 任务拆分、实施顺序和工期。

总览审核通过后，再按 Engine、Admin、Starter、Test 及各项治理能力分别拆分子 Spec，逐步实现。

## 2. 已确认的核心结论

| 决策 | 结论 |
|---|---|
| 项目规模 | 虽然归属 Component 项目，但它是一套独立的大型网关平台，不能按普通轻量 Starter 设计 |
| 原始范围 | `/Users/mario/SelfProject/blog/source/_posts/archtect/gateway.md` 29 章表达的能力全部纳入项目范围 |
| 核心模块 | 建设 Gateway Engine 集群、Gateway Admin 管理平台、Gateway Starter 和 Gateway Test |
| 网关内核 | 优先自研；基于 Reactor Netty/Netty 提供的网络能力建设自己的请求模型、路由、Filter Chain、执行器和生命周期 |
| Spring Cloud Gateway | 只借鉴 Route、Predicate、Filter、治理和可观测性思想，不作为 Engine 运行内核 |
| 分层架构 | Engine、Admin、Starter 均采用分层架构；模块之间通过明确契约交互 |
| 上游协议 | 首期正式支持 HTTP 和 Dubbo；后续协议通过 Adapter SPI 扩展 |
| Admin 定位 | Admin 是管理平台和控制面，负责接口定义存储、配置管理、版本发布、集群管理及管理页面 |
| Starter 定位 | Starter 只采集并上报 Provider 的接口定义，不负责接口调用上报 |
| 调用事件 | 每次接口调用由 Engine 异步发送到 Kafka，不能由 Starter 发送 |
| 接口目录 | 接口按业务域 → 实体域 → 接口组三级组织；一个 Controller/Facade 对应一个接口组 |
| 接口详情 | Starter 需要上报完整接口描述、参数、响应、协议、约束、示例、来源和治理元数据 |
| 外部访问 | 每个 HTTP/Dubbo Operation 都有 `externalAccessible`；false 时仅允许内部入口调用 |
| 鉴权范围 | 不实现具体下游权限系统，但网关必须具备认证、授权、身份上下文和身份透传扩展点 |
| 节点注册 | 当前使用 Nacos 注册和发现 Gateway Node；通过 SPI 隔离，后续迁移自研 DDC |
| 管理前端 | React + TypeScript + Ant Design + Ant Design Charts |
| 流量入口 | Nginx 承担 Gateway Group 的入口和节点负载；区分 PUBLIC 与 INTERNAL 入口 |
| Trace ID | 优先使用前端或调用方生成的合法 Trace ID；没有或不合法时由 Engine 生成 |
| Test 定位 | 参考动态线程池项目，启动真实 HTTP/Dubbo 应用并完成真实注册、可见和调用闭环 |

## 3. 产品定位与边界

### 3.1 产品定位

Gateway 是位于调用方与业务 Provider 之间的统一流量入口，同时包含数据面和控制面：

- 数据面由 Gateway Engine 承担，负责实际接收、治理和转发请求。
- 控制面由 Gateway Admin 承担，负责定义、编排、发布和管理网关能力。
- Gateway Starter 安装在 Provider 中，负责发现和上报接口定义。
- Gateway Test 提供真实应用和环境，持续验证整个闭环。

该项目不是单个反向代理服务，也不是只包装 Dubbo 的协议转换器，而是能够运行多组 Gateway、管理多个 Engine 节点和多个业务系统的网关平台。

### 3.2 首期范围

首期范围包括：

1. 自研 HTTP 网关运行内核。
2. HTTP Provider 转发。
3. Dubbo Provider 泛化调用。
4. 动态路由和版本化发布。
5. Gateway Group 与多 Engine Node 管理。
6. Nacos 节点注册和发现。
7. Nginx 节点负载与动态配置。
8. Provider 接口定义采集、上报和管理。
9. 业务域、实体域、接口组、Operation 管理。
10. PUBLIC/INTERNAL 入口隔离。
11. 网关鉴权扩展框架。
12. 超时、限流、并发隔离、熔断、重试等治理能力。
13. Trace、日志、指标和 Kafka 调用事件。
14. React 管理平台。
15. 真实 HTTP/Dubbo 应用的端到端测试。
16. 容器化部署、优雅停机和故障恢复。

### 3.3 首期不实现的内容

下列内容不属于首期，但不得阻断未来扩展：

1. 具体的下游账号、角色、权限和租户系统。
2. 内置 Shiro、JWT、OAuth2、OIDC 或其他具体认证 Provider。
3. gRPC、WebSocket、GraphQL 等额外协议。
4. 自研 Nginx 替代品。
5. 自研 Nacos 替代品；自研 DDC 接入属于后续迁移。
6. API 商业化计费、开发者门户和开放平台套餐。

“不实现具体权限系统”不等于“网关没有鉴权”。Engine 必须提供完整扩展链，后续权限系统只需实现扩展契约即可接入。

## 4. 总体架构

```mermaid
flowchart LR
    PublicClient["外部调用方"] --> PublicNginx["PUBLIC Nginx 入口"]
    InternalClient["内部调用方"] --> InternalNginx["INTERNAL Nginx 入口"]

    PublicNginx --> GatewayGroup
    InternalNginx --> GatewayGroup

    subgraph GatewayGroup["Gateway Group"]
        Engine1["Gateway Engine 1"]
        Engine2["Gateway Engine 2"]
        EngineN["Gateway Engine N"]
    end

    Engine1 --> HttpProvider["HTTP Provider"]
    Engine1 --> DubboProvider["Dubbo Provider"]
    Engine2 --> HttpProvider
    Engine2 --> DubboProvider
    EngineN --> HttpProvider
    EngineN --> DubboProvider

    Engine1 --> Kafka["Kafka 调用事件"]
    Engine2 --> Kafka
    EngineN --> Kafka

    Engine1 --> Nacos["Nacos"]
    Engine2 --> Nacos
    EngineN --> Nacos

    Starter["Gateway Starter"] --> Admin["Gateway Admin"]
    Starter -. "运行在 Provider 内" .-> HttpProvider
    Starter -. "运行在 Provider 内" .-> DubboProvider

    Admin --> PostgreSQL["PostgreSQL"]
    Admin --> Redis["Redis"]
    Admin <--> Nacos
    Admin --> GatewayGroup
    Admin --> PublicNginx
    Admin --> InternalNginx

    AdminWeb["React Admin Web"] --> Admin
    DDC["自研 DDC（后续）"] -. "替换节点注册 Adapter" .-> GatewayGroup
```

### 4.1 架构原则

1. **控制面与数据面分离**：Admin 故障不能直接中断 Engine 已加载路由的调用。
2. **配置事实与通知分离**：Admin 持久化数据和已发布快照是事实源；消息只用于提示 Engine 尽快拉取。
3. **定义与运行分离**：Starter 上报“Provider 有什么接口”，Admin 决定“哪些接口进入哪个 Gateway、如何暴露和治理”。
4. **入口与暴露策略分离**：PUBLIC/INTERNAL 来自可信物理入口，`externalAccessible` 决定 Operation 是否允许通过 PUBLIC 入口。
5. **内核与基础设施分离**：自研 Core 不依赖 Admin、Nacos、Kafka 或具体权限系统。
6. **可替换扩展点**：上游协议、鉴权 Provider、节点注册中心和配置通知均通过稳定契约隔离。
7. **配置不可变发布**：Engine 消费已发布版本，不直接运行未发布的编辑态配置。
8. **故障保留最后可用状态**：控制面或通知链路不可用时，Engine 使用 Last-Known-Good 配置继续提供服务。

## 5. 项目与模块职责

### 5.1 核心产品模块

#### Gateway Engine

Gateway Engine 是实际处理请求的网关运行时。多个 Engine 组成一个 Gateway Group，可水平扩缩。

主要职责：

- 接收 PUBLIC 和 INTERNAL HTTP 请求；
- 解析 Trace、入口区域、协议和请求参数；
- 匹配已发布路由；
- 执行外部暴露检查、认证、授权和治理 Filter；
- 调用 HTTP 或 Dubbo Provider；
- 生成标准响应、反馈头、日志和指标；
- 将调用事件异步发送到 Kafka；
- 从 Nacos 注册和注销节点；
- 拉取、校验、编译和切换 Admin 发布的配置；
- 在控制面异常时使用最后可用配置继续运行；
- 支持 Drain、优雅停机和连接资源释放。

#### Gateway Admin

Gateway Admin 是网关管理平台和控制面，不承载业务请求转发。

主要职责：

- 管理 Gateway Group、Engine Node 和业务系统；
- 订阅 Nacos 并形成 Gateway Node 管理视图；
- 持久化 Starter 上报的接口定义；
- 管理业务域、实体域、接口组和 Operation；
- 管理 HTTP/Dubbo 路由、外部暴露、鉴权引用和治理策略；
- 形成不可变配置版本并发布给 Engine；
- 展示发布、节点准备、运行版本和异常状态；
- 管理 Nginx 期望负载配置并驱动安全刷新；
- 提供 React 管理页面、图表和审计能力。

#### Gateway Starter

Gateway Starter 安装在 HTTP/Dubbo Provider 中。

主要职责：

- 识别当前应用和环境；
- 扫描 Controller、HTTP Facade 或 Dubbo Facade；
- 生成业务域、实体域、接口组和 Operation 定义；
- 采集完整接口元数据；
- 计算定义指纹并批量上报 Admin；
- 支持幂等、重试、变更和下线语义。

Starter 明确不负责：

- 拦截或统计接口调用；
- 向 Kafka 发送调用事件；
- 决定 Engine 的运行路由；
- 注册 Gateway Engine Node。

#### Gateway Test

Gateway Test 不是 Mock 集合，而是网关平台的真实验证工程。

主要职责：

- 提供可执行 HTTP Provider；
- 提供可执行 Dubbo Provider；
- 两类 Provider 均安装真实 Gateway Starter；
- 真实注册到 Nacos 并向 Admin 上报；
- 提供真实调用客户端和 Kafka Consumer；
- 验证 Admin 可见、Engine 可路由、Nginx 可负载和 Kafka 可消费。

### 5.2 支撑模块

为保持职责清晰，可在核心产品模块内部拆出以下支撑模块：

- `gateway-contract`：模块间稳定 DTO、事件和扩展契约；
- `gateway-core`：请求模型、路由、Filter Chain、参数绑定、执行器和协议 SPI；
- `gateway-admin-web`：React 管理前端；
- 各基础设施 Adapter：Nacos、Kafka、Redis、PostgreSQL、Nginx、HTTP、Dubbo。

支撑模块是工程组织手段，不改变 Engine、Admin、Starter、Test 四类产品职责。最终 Maven 模块名称在对应实施子 Spec 中确定。

### 5.3 分层架构

各运行模块统一采用分层架构，但不强迫所有模块拥有完全相同的包数量：

| 层 | 职责 |
|---|---|
| Interfaces | HTTP、消息、Starter 回调、管理页面接口和协议转换 |
| Application | 用例编排、事务边界、发布流程和生命周期协调 |
| Domain | 路由、接口目录、Gateway Group、Node、策略等核心规则 |
| Infrastructure | PostgreSQL、Redis、Nacos、Kafka、Nginx、HTTP、Dubbo 和文件存储 Adapter |

依赖方向由外向内，Domain 不依赖具体基础设施。Engine 的网络传输层可以直接使用 Reactor Netty，但必须转换为自有 `GatewayExchange` 后再进入应用处理链。

## 6. 完整功能范围

### 6.1 Engine 数据面

#### 网络与会话

- 基于 Reactor Netty/Netty 提供 HTTP Server。
- 支持连接、请求体聚合、流式或聚合响应、协议异常处理。
- 使用自有请求、响应、Exchange 和上下文模型。
- 区分 PUBLIC 与 INTERNAL Listener，不接受客户端伪造入口区域。

#### 路由

- 根据 Gateway Group、配置版本、Host、Path 和 HTTP Method 匹配 Operation。
- 支持精确路径、路径变量和必要的通配规则。
- 支持同一路径不同 HTTP Method。
- 路由定义来自 Admin 已发布快照。
- 支持新版本预加载、原子切换、回滚和旧版本安全退役。

#### HTTP 与 Dubbo 调用

- HTTP Adapter 支持常用 Method、Header、Query、Path、Body 和响应透传/转换。
- Dubbo Adapter 使用泛化调用，不要求 Engine 引入 Provider 接口 JAR。
- 支持 Dubbo interface、method、parameter types、version 和 group。
- HTTP、Dubbo 共享统一执行器、治理链和错误模型。
- 后续协议通过 `UpstreamAdapter` 接入，不修改路由核心。

#### 参数与响应

- 支持 Path、Query、Header、Cookie、Form 和 JSON Body 参数。
- 根据 Starter 上报的 Schema 和参数顺序完成类型绑定。
- 对缺失、类型错误和约束失败返回稳定错误。
- 支持统一网关响应和按 Route 配置的透明响应。
- 禁止调用方通过类型字段要求 Engine 反序列化任意 Java Class。

#### Filter Chain

- 自研可排序、可扩展的 Filter Chain。
- 支持全局、Gateway Group、业务系统、接口组和 Operation 等策略作用域。
- 核心阶段包括 Trace、入口识别、CORS、路由、外部暴露、认证、授权、流控、参数绑定、调用、响应、日志、指标和调用事件。
- Filter 必须有明确顺序、短路行为和异常边界。

#### 流量治理

- 超时；
- 请求速率限制；
- 并发隔离；
- 熔断；
- 有条件重试；
- 请求体大小限制；
- Header 与连接限制；
- Provider 实例选择和健康感知；
- 可扩展灰度、权重和标签路由能力。

#### 运行与恢复

- Readiness 与 Liveness 分离。
- 节点启动后先完成注册和配置准备，再加入流量。
- 支持 Drain 和优雅停机。
- 配置发布失败时保留旧版本。
- Admin、Redis 或通知暂时不可用时，已运行 Engine 继续使用最后可用配置。

### 6.2 Admin 控制面

#### 资源管理

- Gateway Group；
- Gateway Engine Node；
- Application System；
- Business Domain；
- Entity Domain；
- Interface Group；
- Operation；
- HTTP/Dubbo 上游目标；
- Route、暴露策略、鉴权策略引用和治理策略；
- Group、Node 与 System 的关联关系。

#### 接口定义管理

- 接收 Starter 的批量定义上报。
- 持久化三级接口目录和详细 Operation。
- 区分 Provider 原始定义与 Admin 治理配置。
- 展示新增、修改、下线和冲突。
- Starter 上报不能直接把接口变成公网可调用状态。
- 支持没有 Starter 的接口由管理员补充维护，但来源必须可区分。

#### 路由配置与发布

- 编辑态和运行态分离。
- 发布前校验路径冲突、参数定义、Provider 目标、外部暴露和鉴权引用。
- 将一个 Gateway Group 的配置编译成不可变版本。
- Engine 先拉取并准备新版本，再统一切换流量。
- 展示节点版本、准备结果和失败原因。
- 支持历史版本查看、差异和回滚。
- 消息通知丢失后可通过版本对账恢复。

#### 节点与负载管理

- 从 Nacos 获取 Engine Node 的注册和健康信息。
- 保存用于管理、审计和发布判断的节点投影。
- 展示节点所属 Group、地址、版本、状态和最近变化。
- 根据健康、准备和 Drain 状态维护 Nginx 目标节点集合。
- Nginx 配置变更必须先校验，再原子切换和 reload；失败保留旧配置。

#### 管理页面

- 总览仪表盘；
- Gateway Group 与 Node 页面；
- 业务系统和三级接口目录树；
- Operation 详细定义页面；
- 路由、外部暴露、鉴权和治理配置页面；
- 发布、差异、节点准备和回滚页面；
- Nginx 负载与变更状态页面；
- Trace、流量、错误率、延迟和节点分布图表；
- 审计和异常提示。

### 6.3 Starter 接口定义上报

Starter 上报的目录结构为：

```text
Application System
└── Business Domain
    └── Entity Domain
        └── Interface Group
            └── Operation
```

三级分组指 Business Domain、Entity Domain 和 Interface Group；Application System 是接口归属根，Operation 是最终可调用单元。

映射规则：

1. 一个 Controller、HTTP Facade 或 Dubbo Facade 对应一个 Interface Group。
2. 多个 Interface Group 可以归入一个 Entity Domain。
3. 多个 Entity Domain 可以归入一个 Business Domain。
4. 同一个 Controller/Facade 不能在一次上报中拆成多个 Interface Group。
5. 目录 code 必须稳定，名称和描述可以演进。

Operation 至少包含以下信息：

- 稳定 Operation ID、名称、摘要、详细描述、标签和负责人；
- 所属应用、业务域、实体域、接口组；
- HTTP 或 Dubbo 协议类型；
- HTTP Path、Method、Host、consumes 和 produces；
- Dubbo interface、method、version、group 和注册服务信息；
- 参数名称、顺序、来源、类型、泛型 Schema、必填、默认值和校验约束；
- 请求示例；
- 成功响应、错误响应、Schema 和示例；
- `externalAccessible`；
- 鉴权策略引用和可配置治理元数据；
- 废弃标记、版本、构建信息、来源类和方法签名；
- 用于幂等和变更识别的定义指纹。

Admin 必须存储这些信息，而不是只保存 Path、Method 和 Dubbo 方法名。

### 6.4 外部与内部访问

每个 Operation 都必须有 `externalAccessible`：

- `true`：允许通过 PUBLIC 或 INTERNAL 入口继续进入后续处理。
- `false`：PUBLIC 入口按“未对外暴露”处理，不能调用；INTERNAL 入口可以继续处理。

技术原则：

1. 默认值为 false，防止 Starter 新上报接口自动暴露公网。
2. PUBLIC/INTERNAL 身份只由 Engine 监听端口或可信 Nginx 通道确定。
3. 不信任普通客户端传入的“内部请求”Header。
4. 暴露检查在具体鉴权前完成，避免向外部泄漏内部接口存在性。
5. HTTP 和 Dubbo Operation 使用同一规则。
6. `externalAccessible=true` 只代表允许从外部入口进入，不代表跳过鉴权、限流或其他治理。

### 6.5 网关鉴权扩展

首期不实现具体下游权限系统，但 Core 必须定义：

- Credential 提取扩展；
- `GatewayAuthenticationProvider`；
- `GatewayAuthorizationProvider`；
- 不可变 `GatewayPrincipal`；
- `GatewayAuthContext`；
- 认证、拒绝、不可用等稳定决策模型；
- HTTP Header 和 Dubbo Attachment 的受控身份透传；
- Route 对鉴权策略和 Provider ID 的引用。

基本规则：

1. Operation 可以明确配置无需鉴权，但不能因 Provider 缺失而自动放行。
2. 配置了鉴权策略却找不到 Provider 时，发布应失败；运行期异常时默认关闭访问。
3. 客户端传入的内部身份 Header 必须先移除，再由可信 Mapper 写入。
4. 认证失败、授权失败和 Provider 不可用需要可区分。
5. 未来接入公司的权限系统时，只实现 Provider 和 Mapper，不改 Engine 路由核心。

### 6.6 Trace 与调用事件

Trace ID 规则：

1. 前端或调用方负责优先生成 Trace ID。
2. Engine 校验并接受合法的 `traceparent` 或约定 Trace Header。
3. 缺失或非法时由 Engine 生成。
4. Trace ID 贯穿 Engine 日志、HTTP Header、Dubbo Attachment、响应、指标和 Kafka 调用事件。
5. Admin React 前端对管理请求也使用统一 Trace 生成和透传策略。

调用事件规则：

1. 只有 Engine 产生接口调用事件。
2. Starter 不拦截调用，也不持有 Kafka Producer。
3. 调用完成后异步发送 Kafka，Kafka 故障不能改变业务响应。
4. 事件包含 Trace、Route、Operation、Node、耗时、结果和必要的诊断维度。
5. 默认不记录请求/响应 Body、凭据和敏感身份信息。
6. 投递链路需要有界、可观测，并在后续子 Spec 中确定缓冲和可靠性级别。

## 7. 自研网关技术路线

### 7.1 路线比较

| 路线 | 说明 | 结论 |
|---|---|---|
| Spring Cloud Gateway 扩展 | 直接使用其 Server、RouteDefinition 和 GlobalFilter，再扩展 Dubbo | 可借鉴但不作为主路线，核心路由和生命周期受框架约束 |
| Reactor Netty 上自研内核 | 复用成熟网络传输和 HTTP 编解码，自研 Exchange、路由、Filter、版本和 Adapter | 推荐，既满足自研要求，又避免重复实现底层协议 |
| 原始 Netty 全自研 | 从 Pipeline、HTTP 聚合、连接管理到 Client 全部自行建设 | 首期成本和风险过高，不推荐 |

推荐路线是“Reactor Netty 基础设施 + 自研 Gateway Core”，不是 Spring Cloud Gateway 二次封装。

### 7.2 自研与复用边界

项目自行实现：

- `GatewayRequest`、`GatewayResponse`、`GatewayExchange`；
- 路由定义、路由编译器和 Matcher；
- 版本化 Route Repository；
- Filter Chain 和 Filter 扩展契约；
- 入口区域与外部暴露判断；
- 鉴权扩展链和身份上下文；
- 参数提取、类型绑定和响应映射；
- `GatewayExecutor`；
- `UpstreamAdapter` 和调用生命周期；
- 错误模型、反馈头和调用事件；
- 路由 Prepare、Activate、Retire 与 LKG；
- Engine Node 生命周期和控制面协作。

项目复用：

- Reactor Netty/Netty 的 EventLoop、Channel、HTTP 编解码、连接池和 HttpClient；
- Dubbo 的 `GenericService`；
- Jackson 的 JSON 与 Schema 基础能力；
- Resilience4j 或等价成熟库的限流、熔断和重试算法；
- Micrometer/OpenTelemetry 的指标和 Trace 标准；
- Nacos Client、Kafka Client、Redis Client 和 PostgreSQL Driver。

Engine 运行依赖中不引入 `spring-cloud-starter-gateway-*`，也不把 Spring Cloud Gateway 的 `RouteDefinition`、`GlobalFilter` 或 Actuator API 当成项目契约。

### 7.3 请求处理主链

```mermaid
flowchart LR
    Transport["Reactor Netty Transport"]
    Trace["Trace Context"]
    Zone["Access Zone"]
    Cors["CORS"]
    Match["Route Match"]
    Exposure["External Exposure"]
    Authentication["Authentication"]
    Authorization["Authorization"]
    Governance["Limit / Timeout / Circuit"]
    Binding["Parameter Binding"]
    Executor["Gateway Executor"]
    Adapter["HTTP / Dubbo Adapter"]
    Response["Response Mapping"]
    Observe["Log / Metric / Kafka Event"]

    Transport --> Trace --> Zone --> Cors --> Match --> Exposure
    Exposure --> Authentication --> Authorization --> Governance
    Governance --> Binding --> Executor --> Adapter --> Response --> Observe
```

这是逻辑阶段，不代表每个阶段都必须拆成一个类。实施时只在确有变化点的地方使用扩展接口，避免为了模式而过度设计。

### 7.4 设计模式选择

| 模式 | 使用位置 | 原因 |
|---|---|---|
| Chain of Responsibility | Filter、认证、授权和治理处理链 | 阶段有顺序、可短路且需要扩展 |
| Strategy | 路由策略、限流键、鉴权 Provider 和节点选择 | 同一职责存在可配置算法 |
| Adapter | HTTP、Dubbo、Nacos、未来 DDC | 隔离外部协议和基础设施 |
| Factory/Builder | 编译不可变 Route Snapshot 和调用资源 | 创建过程需要集中校验 |
| State | Route 发布和 Engine Node 生命周期 | 状态变化具有明确约束 |
| Facade | `GatewayExecutor` | 为网络层提供单一稳定执行入口 |

简单的参数转换和直接业务判断不额外引入模式。

## 8. 路由、发布与集群技术路线

### 8.1 路由配置

- Admin 保存接口定义和治理配置。
- 发布时按 Gateway Group 生成完整、不可变、带版本和校验值的 Route Snapshot。
- Engine 拉取 Snapshot，在流量外完成校验和编译。
- 编译成功后进入可切换状态，失败不影响当前版本。
- Group 内目标节点准备完成后再切换入口版本。
- 历史回滚通过重新发布一个已知内容的新版本完成。

Redis 可用于变更通知、热点缓存、分布式协调和限流，但不能成为配置唯一事实源。即使通知丢失，Engine 也能通过版本对账发现新配置。

### 8.2 Gateway Group 与 Engine Node

- 一个 Gateway Group 包含多个 Engine Node。
- 业务系统或路由被分配到 Gateway Group，而不是绑定单台 Engine。
- Group 内 Node 应运行同一已激活配置版本。
- 新 Node 完成 Nacos 注册、配置加载和资源预热后才能接流。
- Drain、异常、未准备或已注销的 Node 不进入 Nginx 可用集合。

### 8.3 Nacos 与未来 DDC

当前路线：

- Engine 通过 `GatewayNodeRegistry` Port 注册、更新元数据和注销。
- Infrastructure 提供 Nacos Adapter。
- Nacos 临时实例和健康状态作为 Node 存活事实。
- Admin 订阅 Nacos 并持久化管理投影。
- Provider 的 HTTP/Dubbo 运行实例也可以使用 Nacos，但与 Gateway Node 使用隔离的 serviceName、group 和 namespace。

未来迁移：

- 自研 DDC 实现同一 Node Registry 契约。
- Engine Domain 和路由核心不感知 Nacos/DDC 差异。
- 迁移期可增加双注册或灰度读取，但具体方案留到 DDC 接入子 Spec。

### 8.4 Nginx

Nginx 负责：

- 提供 PUBLIC 与 INTERNAL 入口；
- 按 Gateway Group 将流量分配到健康、已准备的 Engine Node；
- 承载 TLS、基础连接保护和入口级策略；
- 在路由版本切换时把一个入口稳定导向兼容版本。

Admin 负责生成 Nginx 期望配置并驱动受控发布。配置必须经过渲染、校验、原子替换、reload 和失败回滚，不能让 Admin 获得不受限的主机命令或 Docker Socket 权限。

## 9. Admin 技术路线

### 9.1 后端

- Java 21、Spring Boot 和分层架构；
- PostgreSQL 作为接口定义、治理配置、发布版本和审计的持久化事实源；
- Redis 用于缓存、通知、协调和限流辅助，不保存 Node 存活事实；
- Nacos Client 用于订阅 Gateway Node；
- Kafka 可用于平台事件集成，但接口调用事件始终由 Engine 生产；
- 管理 API 与 Engine 内部控制 API 分离；
- 管理面部署在受信任网络，具体账号/RBAC 作为独立后续能力。

### 9.2 前端

- React；
- TypeScript；
- Vite；
- Ant Design；
- Ant Design Charts；
- 统一请求客户端、Trace ID、错误处理和权限占位能力；
- 以目录树、详情页、发布工作台和图表为主要交互模型。

Ant Design Charts 主要展示节点、流量、错误、延迟、发布状态和版本分布。图表只展示有明确来源的指标，不以高基数原始 Path 或 Trace ID 作为指标标签。

## 10. Starter 技术路线

Starter 采用“声明 + 扫描 + 编译 + 批量上报”路线：

1. Provider 声明应用、业务域、实体域和接口组信息。
2. Starter 扫描 Spring Controller、HTTP Facade 或 Dubbo Facade。
3. 结合注解、方法签名、参数类型和校验注解编译 Operation。
4. 对整个上报批次和单个定义计算稳定指纹。
5. 应用启动成功后向 Admin 批量上报。
6. Admin 在一个一致性边界内保存目录和接口详情。
7. 重复上报幂等，定义变化可识别，下线有明确语义。

Starter 默认是否开启、注解命名、OpenAPI 元数据复用方式和失败策略在 Starter 子 Spec 中确定。无论最终选择如何，Starter 上报失败不能静默把未审核接口变成公网路由。

## 11. Test 技术路线

Gateway Test 参考动态线程池项目的“真实示例应用 + 可选真实环境测试”方式。

建议包含以下真实运行单元：

- HTTP Provider：真实 Spring Boot Controller，安装 Gateway Starter；
- Dubbo Provider：真实 Dubbo Facade 与实现，安装 Gateway Starter；
- Gateway Client：模拟前端生成 Trace ID，并调用 PUBLIC/INTERNAL 入口；
- Kafka Consumer：验证 Engine 发送的调用事件；
- E2E：编排 Admin、Engine、Nacos、PostgreSQL、Redis、Kafka、Nginx 和 Provider。

验证目标：

1. HTTP/Dubbo Provider 能真实启动。
2. Provider 运行实例能出现在 Nacos。
3. Starter 上报后，Admin 能真实看到业务域、实体域、接口组、Operation 和详细定义。
4. Engine Node 能真实注册并出现在 Admin。
5. Admin 发布后，Engine 能真实加载并调用 HTTP/Dubbo Operation。
6. `externalAccessible=false` 的接口外部不可调、内部可调。
7. 调用方 Trace ID 能贯穿调用；缺失时 Engine 能补充。
8. Engine 能向 Kafka 发送真实调用事件，Starter 不发送。
9. 多 Engine Node 能被 Nginx 真实负载。

日常构建保留快速单元和组件测试；依赖完整基础设施的 Live Test 使用显式环境开关运行。当前阶段只定义测试路线，不启动任何项目。

## 12. 原始 29 章能力对齐

| 章 | 原始能力 | 本项目功能归属 |
|---|---|---|
| 1 | HTTP 请求会话协议处理 | 自研 Reactor Netty Server、Gateway Exchange 和协议处理 |
| 2 | 代理 RPC 泛化调用 | Dubbo `GenericService` Adapter |
| 3 | 分治处理会话流程 | 自研 Filter Chain、Executor 和职责分层 |
| 4 | 将 RPC、HTTP、其他连接抽象为数据源 | `UpstreamAdapter` SPI，首期 HTTP + Dubbo |
| 5 | HTTP 请求参数解析 | 多来源参数提取、Schema 校验和类型绑定 |
| 6 | 执行器封装服务调用 | `GatewayExecutor` 与统一调用结果 |
| 7 | Shiro + JWT 权限认证 | 不内置 Shiro/JWT；实现认证、授权、Principal 和 Provider SPI |
| 8 | 网关会话鉴权处理 | Filter Chain 中的认证、授权、拒绝和身份透传阶段 |
| 9 | 网关注册中心服务创建 | Gateway Admin 控制面 |
| 10 | 网关注册中心库表结构 | Admin 持久化模型；表结构在后续 Admin 子 Spec 设计 |
| 11 | 注册 Gateway 算力节点 | Gateway Group、Engine Node 和 Nacos 注册 |
| 12 | 注册应用、接口、方法 | Admin 的应用与三级接口目录管理 |
| 13 | 服务发现和注册网关连接 | Nacos `GatewayNodeRegistry` 和 Admin 控制面连接 |
| 14 | 网关映射聚合查询 | Gateway Group Route Snapshot 聚合 |
| 15 | 配置拉取和组件验证 | Engine 拉取、校验、编译、准备、切换和反馈 |
| 16 | 网络通信配置提取 | Engine/Admin/Starter 类型化配置 |
| 17 | 核心通信组件管理和服务映射 | Engine 生命周期与版本化 Route Repository |
| 18 | 容器关闭监听和异常管理 | Drain、优雅停机、稳定错误和资源释放 |
| 19 | 网关引擎镜像部署 | Engine 可执行包、容器镜像和部署配置 |
| 20 | 服务注册组件采集接口信息 | Starter 扫描并编译完整接口定义 |
| 21 | 应用服务接口注册到中心 | Starter 批量上报，Admin 持久化 |
| 22 | 订阅服务注册消息驱动网关映射 | 版本通知、全量拉取和周期对账 |
| 23 | 网关运营管理后台 | Admin 后端与 React/Ant Design/Ant Design Charts |
| 24 | 前后端分离跨域调用 | Admin CORS 与 Engine 自研 CORS Filter |
| 25 | Nginx 负载模型配置 | Gateway Group Upstream 与双入口模型 |
| 26 | 动态刷新 Nginx 负载配置 | 校验、原子发布、reload 和失败回滚 |
| 27 | Gateway 节点动态负载 | Nacos Node 变化、准备状态和 Nginx 对账 |
| 28 | 网关组件工程模块合并 | Engine、Admin、Starter、Test 及支撑模块的 Component 聚合工程 |
| 29 | 算力关联、接口上报、调用反馈 | Group/Node/System 关联、三级接口目录、Starter 上报、节点反馈和 Engine Kafka 调用事件 |

结论：29 章能力全部保留。第 7、8 章改变的是具体实现边界，不是删除鉴权能力。

## 13. 本轮审核项

本轮只需审核以下总览结论：

1. Gateway 是大型 Component 项目，包含 Engine 集群、Admin、Starter 和 Test。
2. 原始 29 章能力全部进入最终范围。
3. Engine 采用 Reactor Netty/Netty 之上的自研网关内核，不依赖 Spring Cloud Gateway 运行时。
4. HTTP 与 Dubbo 是首期正式协议。
5. Admin 是管理与控制面，Starter 只上报接口定义，Engine 才发送 Kafka 调用事件。
6. 接口目录是业务域 → 实体域 → 接口组，Controller/Facade 与接口组一一对应。
7. Admin 保存完整 Operation 详情。
8. `externalAccessible=false` 只能从 INTERNAL 入口调用。
9. 不实现具体下游权限系统，但必须建设网关鉴权扩展框架。
10. Gateway Node 当前注册到 Nacos，后续通过 Adapter 迁移 DDC。
11. Admin 前端使用 React、Ant Design 和 Ant Design Charts。
12. Test 必须启动真实 HTTP/Dubbo 应用，并在 Admin 中真实可见。
13. Trace ID 由前端/调用方优先生成，缺失或非法时 Engine 生成。

上述总览审核通过后，再拆分实施级子 Spec；在此之前不创建 Gateway 模块、不修改 POM、不引入依赖，也不启动项目。
