# GWS-10 Gateway Starter 接口定义上报 Spec

状态：草案，等待审核

父文档：`2026-07-24-gateway-component-design.md`

索引：`2026-07-25-gateway-child-spec-index.md`

依赖：GWS-01、GWS-02、GWS-09

## 1. 目标

`gateway-starter` 安装在下游 HTTP/RPC Provider 应用中，只负责发现、规范化并向
Gateway Admin 上报接口定义。

必须明确区分：

```text
接口定义上报：Starter → Gateway Admin
HTTP 实例注册：Provider Runtime → DDC
RPC 实例注册：RPC Component → DDC
接口调用事件：Gateway Engine → Kafka
```

Starter 不拦截业务调用、不采集调用记录、不发送 Kafka、不维护 Provider 租约，也不
承担接口调用客户端职责。

## 2. 支持范围

### 2.1 HTTP

首期支持 Spring 应用实际注册的：

- Spring MVC Controller；
- Spring WebFlux Annotated Controller；
- `@RequestMapping` 及组合注解；
- Jackson 可描述的请求/响应模型；
- Jakarta Validation 约束；
- 常见参数来源：Path、Query、Header、Cookie、Body、Multipart。

Starter 从框架最终 `RequestMapping` 模型读取，而不是只扫描注解文本，避免类级与
方法级映射合并、组合注解和条件映射漂移。

### 2.2 RPC

首期只支持 Egon RPC 的 gRPC + Protobuf Unary Provider：

- 从 GWS-02 `RpcContractCatalog` 读取已校验 Contract；
- 使用标准 `FileDescriptorSet` Snapshot；
- 不二次扫描 Provider Bean；
- 不支持 Streaming；
- 不支持 Dubbo。

### 2.3 明确不做

- 不上报每次接口调用；
- 不探测 Provider Host/Port；
- 不注册 `HTTP_PROVIDER` 或续租；
- 不向 DDC 写接口定义；
- 不自动创建公开 Route；
- 不根据接口注解配置限流、熔断或业务鉴权；
- 不上传方法实现、Java 字节码、源代码或 Secret；
- 不依赖 Nacos；
- 不要求所有下游应用安装 Springdoc。

## 3. 三级分组

目录固定为：

```text
Business Domain
└── Entity Domain
    └── Interface Group
        └── Operations
```

规则：

1. 每个 Controller 对应一个 Interface Group；
2. 每个 RPC Contract/Proto Service 对应一个 Interface Group；
3. 多个 Interface Group 可归入同一 Entity Domain；
4. 多个 Entity Domain 可归入同一 Business Domain；
5. Controller 的多个方法是同一 Interface Group 下的 Operation；
6. 分组 Code 是稳定身份，Name/Description 是展示信息；
7. Controller 重命名不能在没有明确 Code 迁移的情况下静默复用原 Group。

## 4. 配置与注解

### 4.1 应用级配置

```yaml
egon:
  cola:
    component:
      gateway:
        reporting:
          enabled: true
          admin-base-url: https://gateway-admin.internal
          application-code: order-service
          application-name: Order Service
          env: prod
          namespace: default
          artifact-version: 2.3.1
          build-id: ${BUILD_ID}
          declared-hosts: []
          fail-fast: false
```

HMAC Access Key/Secret 使用安全配置注入，不显示在示例明文中。

必填稳定字段：

- `application-code`；
- `env`；
- `namespace`；
- `artifact-version`；
- `build-id`。

`build-id` 必须标识一次不可变构建，不能使用启动时间或随机 UUID 代替。

Spring Handler Mapping 通常不包含部署 Host。`declared-hosts` 只上报 Provider 声明的
合法 Host 约束；为空时不生成“任意公网 Host”，Admin 创建 Route 时仍须显式配置
Host/作用域。

### 4.2 分组注解

提供最小注解：

```java
@Target(TYPE)
public @interface GatewayInterfaceGroup {
    String businessDomainCode();
    String businessDomainName();
    String entityDomainCode();
    String entityDomainName();
    String code();
    String name();
    String description() default "";
}
```

HTTP Controller 和 RPC Provider Contract 均使用该分组语义。对于无法修改源码的
第三方 Controller，可通过配置映射补充：

```yaml
egon.cola.component.gateway.reporting.group-mappings:
  - type: com.example.order.OrderQueryController
    business-domain-code: trade
    entity-domain-code: order
    interface-group-code: order-query
```

同一个 Type 同时存在注解与配置时，配置只允许补充展示字段；稳定 Code 冲突时启动
校验失败，不能静默覆盖。

### 4.3 Operation 补充注解

```java
@Target(METHOD)
public @interface GatewayOperation {
    String name() default "";
    String summary() default "";
    String description() default "";
    String owner() default "";
    boolean externalAccessible() default false;
    String[] tags() default {};
}
```

`externalAccessible` 默认 false。该字段只上报接口是否允许配置为外部可调用；Starter
不会创建 PUBLIC Route。

RPC 方法未单独注解时也会从 Descriptor 上报，但 `externalAccessible=false`。

## 5. HTTP 定义采集

### 5.1 采集源

在应用完成 Handler Mapping 注册后读取：

- Bean Type 和用户 Class；
- 合并后的 HTTP Method、Path、Consumes、Produces；
- 应用配置声明的 Host 约束；
- Path/Header/Param 条件；
- 方法参数及解析注解；
- 返回类型；
- Jackson Property；
- Jakarta Validation；
- Deprecated 与 Gateway 注解。

不把 Spring 内部管理 Endpoint、错误 Handler、Actuator 或 Starter 自身 Endpoint
上报，排除规则必须显式并可测试。

### 5.2 多映射

一个 Handler Method 若对应多个 Method/Path 组合，每个规范化组合形成独立
Operation。禁止把多个 Path 放入一个模糊 Operation。

示例：

```text
GET /orders/{id}
HEAD /orders/{id}
```

对应两个 Operation Key。

### 5.3 参数

每个参数至少包含：

```text
name
location = PATH | QUERY | HEADER | COOKIE | BODY | PART
required
javaTypeDisplay
schema
defaultValue
constraints
description
```

约束：

- PATH 参数始终 required；
- 隐式框架参数，如 Request、Response、Principal，不作为业务参数；
- 一个 Operation 最多一个逻辑 Body；
- 上传只描述 Content Type、字段和大小约束，不读取文件；
- 泛型、数组、枚举和嵌套对象保留 Schema；
- 无法稳定描述的类型产生校验警告或按严格模式失败。

### 5.4 响应与错误

记录：

- Operation 名称、摘要、详细描述、标签、负责人；
- 来源 Type 与稳定方法签名字符串；
- 成功 HTTP Status；
- Content Types；
- 响应 Body Schema；
- 无 Body 响应；
- 已声明的错误模型/状态；
- 受控请求、成功响应和错误响应示例；
- Deprecated 信息；
- 流式响应标记。

示例可以来自项目已存在的 OpenAPI 注解或 Gateway 专用示例声明，但 Starter 不要求
安装 Springdoc。示例按 Schema 校验并限制单项/总字节数，不能包含真实 Token、Cookie、
手机号等生产数据；非法示例按严格模式失败或产生警告。

首期 Engine 不承诺代理所有流式类型。发现不支持的 Streaming/SSE 时定义仍可上报，
但标记 `gatewaySupport=UNSUPPORTED`，Admin 禁止为其发布 Route。

## 6. RPC 定义采集

每个 `RpcContractSnapshot` 转换为：

```text
Interface Group
├── protocol = RPC
├── serviceName/group/version
├── protoPackage/protoServiceName
├── descriptorSha256
├── fileDescriptorSet
└── Operations
    ├── fullMethodName
    ├── requestType
    ├── responseType
    ├── rpcType = UNARY
    ├── examples
    └── externalAccessible
```

规则：

- Contract Catalog 是唯一来源；
- 一个 Proto Service/Contract 对应一个 Interface Group；
- Method Identity 使用 GWS-01 Operation Key；
- Descriptor 依赖完整、顺序稳定；
- Snapshot 不含 Java Class/Method/Bean；
- RPC Group Code 必须显式提供或从稳定 Service Identity 确定生成；
- 外部 HTTP→RPC 暴露仍由 Admin Route 和映射配置决定。

## 7. 上报模型

### 7.1 完整批次

一次上报是一个不可变、完整的 `GatewayInterfaceDefinitionReport`：

```text
contractVersion = v1
reportId
reportedAt
application
build
complete = true
definitionSetId
definitionFingerprint
businessDomains[]
```

其中：

```text
application = applicationCode + name + env + namespace
build       = artifactVersion + buildId + framework/runtime metadata
```

`complete=true` 表示该批次完整描述本次构建中 Starter 能发现的全部受支持接口，不
表示旧版本 Provider 已退出运行。

### 7.2 Definition Set ID

```text
definitionSetId = SHA-256(
  applicationCode + env + namespace + artifactVersion + buildId
  + definitionFingerprint
)
```

`definitionFingerprint` 对规范化业务定义计算；排除：

- Report ID/时间；
- Provider Host/Port/Instance；
- 非稳定扫描顺序；
- JVM 地址；
- HMAC；
- 运行统计。

相同构建和定义必须产生相同 ID；同一 Build ID 产生不同 Fingerprint 时 Admin 拒绝，
防止可变构建。

Starter 以 GWS-01 `operationKey` 上报稳定自然身份。首次发现时不自行生成
`operationId`；Admin 创建 UUIDv7 并作为公共资源 ID。后续 Report 可以携带上次确认
的 Operation ID，但 ID 与 Key 不一致时以 Admin 事实为准并拒绝冲突。

### 7.3 Report ID

`reportId` 是一次传输幂等键。重试必须使用同一 Report ID 和 Payload。新的应用启动
可以使用新 Report ID，但 Definition Set ID 保持不变。

## 8. Rolling Deployment 与下线语义

仅凭一个新构建缺少某 Operation，不能判断旧接口已经下线，因为滚动发布期间旧
Provider 仍可能存在。

Admin 对 Definition Set 使用：

```text
REPORTED
→ VERIFIED
→ ACTIVE
→ RETIRED
```

规则：

1. 合法完整上报进入 VERIFIED；
2. 自动激活要求存活 Provider Metadata 明确携带相同
   `gateway.definition-set-id`；
3. 若无法建立 Provider 与 Definition Set 的关联，则由 Admin 人工激活；
4. 新 Set 激活后，旧 Set 只在没有对应活跃 Provider 时 RETIRED；
5. Operation 只有在所有活动 Definition Set 都缺失且未被活动 Route 引用时，才可
   进入 OFFLINE；
6. 单次上报失败、Admin 不可用或 Provider 短暂掉线不能下线接口。

Starter 暴露只读 `GatewayDefinitionIdentity` Bean：

```text
definitionSetId
definitionFingerprint
artifactVersion
buildId
```

- 该小型值对象定义在 `gateway-contract`，不依赖 Starter 实现类；
- HTTP Provider Runtime 若同时安装，可把该身份作为非敏感 DDC Metadata 上报；
- Gateway 提供的 RPC Metadata Contributor 可以读取同一 Bean；
- Starter 自己不创建、不续租任何 Lease；
- Provider Runtime 未安装 Starter 时可通过显式配置提供关联 ID。

## 9. 上报时机与生命周期

### 9.1 时机

```text
ApplicationReady
→ 等待 RpcContractCatalog/Handler Mapping 稳定
→ 扫描并本地校验
→ 生成 Canonical Report
→ HMAC 上报
→ 保存结果状态
```

默认不阻塞业务应用启动。`fail-fast=true` 只适用于测试或有严格发布门禁的环境：
扫描/上报失败使应用启动失败。

### 9.2 重试

- 只重试连接失败、超时、429 和 5xx；
- 4xx Schema/签名错误不盲目重试；
- 指数退避、随机抖动和最大间隔；
- 重试使用同一 Report ID/Payload；
- 进程退出取消未完成任务；
- 不使用无界队列；
- Admin 恢复后最终上报。

### 9.3 本地状态

可在应用工作目录保存最近成功的：

- Definition Set ID；
- Fingerprint；
- Report ID/结果；
- Admin 接收时间。

不得保存 HMAC Secret 或完整接口 Payload 的敏感副本。相同 Definition Set 已成功且
Admin 可确认时可避免重复上传大 Descriptor，但应用启动至少执行一次状态确认。

## 10. Admin 机器接口

使用 GWS-09：

```text
POST /api/v1/gateway/openapi/interface-definitions/reports
GET  /api/v1/gateway/openapi/interface-definitions/reports/{reportId}
```

Starter 使用 Spring `RestClient` 和公共 HMAC Signer，在独立有界上报 Worker 中调用；
即使安装在 WebFlux Provider 中也不能阻塞其 EventLoop。连接、读取和总重试时间均有
上限。

Header 遵循 DDC HMAC 的统一规则，并增加：

```text
X-Gateway-Contract-Version
X-Gateway-Application-Code
X-Gateway-Report-Id
```

Admin 响应：

```text
GatewayInterfaceDefinitionReportResult
├── reportId
├── definitionSetId
├── status = ACCEPTED | ACCEPTED_WITH_WARNINGS | REJECTED
├── applicationId
├── counts
│   ├── businessDomains
│   ├── entityDomains
│   ├── interfaceGroups
│   ├── operations
│   ├── created
│   ├── updated
│   └── missingFromThisSet
├── operationRefs[]
│   ├── operationKey
│   ├── operationId
│   └── changeType
├── warnings[]
└── receivedAt
```

返回 `missingFromThisSet` 只表示集合差异，不代表已下线。

## 11. 校验规则

Starter 本地和 Admin 服务端都执行：

- Application/Domain/Group Code 格式与长度；
- 分组父子关系完整；
- 同一父级 Code 唯一；
- Operation Key 唯一；
- HTTP Method/Path 规范；
- Path Variable 一致；
- RPC Descriptor SHA 和 Unary 类型；
- Schema 深度、节点数、总字节上限；
- `externalAccessible` 显式默认 false；
- 不支持协议标记；
- 不包含 Secret、运行地址或调用数据；
- Canonical Fingerprint 可复现。

本地通过不代表 Admin 必须接受；Admin 还校验作用域、Application 身份和已有不可变
构建约束。

## 12. 兼容性

- API `contractVersion` 与 Definition Schema 独立版本化；
- 新增可选字段不得改变旧 Fingerprint 规则，除非提升 Schema Version；
- 未知必需字段/枚举拒绝；
- Spring MVC/WebFlux 使用统一中间模型；
- RPC Snapshot 直接复用 GWS-02，不复制另一套 Descriptor；
- Starter 版本与 Engine 版本没有直接运行依赖；
- Admin 应允许受支持范围内多个 Starter 小版本并存。

## 13. 安全与隐私

- 生产上报必须 HMAC；
- Nonce、时间戳、防重放与 Body SHA 强制开启；
- Application Access Key 只能上报授权的
  `applicationCode + env + namespace`；
- 不上报请求样例中的真实值；
- 注解 Description 视为可见管理数据，团队不得写 Secret；
- Schema 默认不包含字段默认 Secret；
- 日志只记录 Report/Definition Set ID、计数和错误路径；
- 拒绝包含私钥、Token、证书等高风险 Metadata Key。

## 14. 可观测性

Starter 暴露：

```text
gateway_interface_report_total{status,protocol}
gateway_interface_report_duration_seconds{status}
gateway_interface_definition_count{protocol}
gateway_interface_report_retry_total{reason}
gateway_interface_last_success_timestamp
```

Application/Path/Operation 不作为指标 Label。健康信息可以报告最近状态，但默认不因
Admin 暂时不可用把业务应用标记为不健康。

## 15. 测试设计

### 15.1 HTTP 扫描

1. 类级 + 方法级映射合并；
2. 组合注解和多个 Method/Path；
3. Path/Query/Header/Cookie/Body/Multipart；
4. Jackson 嵌套、枚举、数组、泛型；
5. Jakarta Validation；
6. MVC 与 WebFlux 产生一致中间模型；
7. Actuator/错误/Internal Handler 排除；
8. SSE/Streaming 标记不支持；
9. Controller 一对一 Interface Group；
10. 分组冲突启动失败。

### 15.2 RPC 扫描

1. 只读取已验证 Catalog；
2. 每个 Contract 一个 Interface Group；
3. Descriptor 和传递依赖完整；
4. Unary Method Operation Key 稳定；
5. 不存在 Java 反射字段；
6. 默认不允许外部访问。

### 15.3 Canonical 与上报

1. 扫描顺序变化不改变 Fingerprint；
2. 描述变化和协议变化按规则影响 Fingerprint；
3. 相同 Build ID 不同 Fingerprint 被拒绝；
4. Report 重试幂等；
5. HMAC 篡改、过期、Nonce 重放；
6. Admin 5xx 重试、4xx 停止；
7. Fail Fast 开关；
8. Admin 不可用不阻塞默认业务启动；
9. 超限 Schema 在发送前失败；
10. 日志不包含 Payload/Secret。

### 15.4 Rolling Deployment

1. 新旧 Definition Set 同时存在；
2. 新 Set 缺少旧 Operation 不立即下线；
3. Provider Metadata 匹配后激活；
4. 旧 Provider 退出后旧 Set 退休；
5. 活动 Route 阻止 Operation Offline；
6. Starter 与 Provider Runtime 分别安装仍保持职责独立。

## 16. 验收标准

1. Admin 能真实看到 HTTP/RPC 应用的三级目录和详细接口定义；
2. 每个 Controller/Proto Service 恰好对应一个 Interface Group；
3. `externalAccessible` 默认 false 并包含在定义中；
4. Definition Set 对同一不可变构建可复现、可幂等；
5. 滚动发布不会因接口暂时缺失误下线；
6. Starter 不注册 Provider、不拦截调用、不发送 Kafka；
7. HTTP Provider Runtime 和 RPC Component 可选择读取 Definition Set 身份；
8. Admin 不可用时默认不影响业务服务，但能受控重试；
9. 上报使用 HMAC 且不包含 Secret/调用数据；
10. HTTP 与 RPC 都经过真实应用集成测试。

## 17. 本轮审核项

1. 认可 Controller/Proto Service 一对一映射 Interface Group；
2. 认可注解提供稳定三级目录，配置只处理无法改源码的类型；
3. 认可 Definition Set 是“单次构建完整”，但不直接声明旧版本下线；
4. 认可通过 Provider Metadata 或人工确认激活 Definition Set；
5. 认可 Starter 默认上报失败不阻断业务启动、测试可开启 Fail Fast；
6. 认可 Starter 仅上报定义，与 Provider 租约和调用事件严格分离。
