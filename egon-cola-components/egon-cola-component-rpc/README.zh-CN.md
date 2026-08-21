# Egon COLA RPC

[English](README.md) | [中文](README.zh-CN.md)

`egon-cola-component-rpc` 是 Egon COLA 的 RPC 传输组件。它将生成的
Protobuf/gRPC Service 绑定到 Java 接口，提供 Spring Boot Provider 和 Consumer
生命周期，并通过动态配置中心（DDC）接入服务租约与发现。

本组件不是 Gateway 数据面。生产 Gateway、路由规则、Provider 健康策略、流量治理以及
HTTP/RPC 转发由独立的 [Gateway 平台](../../egon-cola-platforms/egon-cola-platform-gateway/README.zh-CN.md)
负责。

## Badges

![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![gRPC 1.75](https://img.shields.io/badge/gRPC-1.75.0-244C5A)
![Protobuf 4.32](https://img.shields.io/badge/Protobuf-4.32.0-4285F4)

## Features

- 使用标准 gRPC Java 和 Protobuf 传输；`.proto` 仍是唯一的线协议 IDL。
- 严格校验 Java 方法、生成的 gRPC Descriptor 以及 Protobuf 请求/响应类型的一致性。
- 通过 `@EgonRpcProvider` 扫描 Provider Bean，管理 unary gRPC Server、可用性门控、
  DDC 租约注册、心跳、恢复和精确租约注销。
- Consumer CGLIB Proxy 支持两条运行路径：经发现的内部 Gateway 调用
  `@EgonRpcReference`，或经发现的 Provider 调用 `@EgonRpcDirectReference`。
- 通过 `RpcDirectClientFactory` 创建基础设施端点的程序化直连 gRPC Client；返回的
  Channel Handle 由调用方负责关闭。
- 支持阻塞和 `CompletionStage` 调用、受限的泛化 raw-Protobuf、HTTP/2 Channel 多路复用，传播 Deadline 和 Cancellation，传递有界 Trace/Request Metadata，并将 gRPC Status
  稳定映射为 `EgonRpcErrorCode`。
- 可选 DDC RPC Adapter，提供 ConfigData、服务注册中心和管理客户端，并支持显式 mTLS
  或开发明文模式，以及按能力隔离的 HMAC 凭据。
- 生成包含依赖文件的 Protobuf Descriptor Snapshot，供 Gateway Reporting Starter 使用，
  不额外创建第二套 Schema 或序列化模型。

## Architecture

### 运行拓扑

```text
业务 Consumer
  ├─ @EgonRpcReference ──> 有效 INTERNAL_GATEWAY 集合 ──> RPC_PROVIDER ──> Provider
  └─ @EgonRpcDirectReference ──> DDC RPC 注册中心 ──> RPC_PROVIDER ──> Provider

Provider ── 注册 / 心跳 / 注销 ─┐
Gateway  ── 注册 / 心跳 / 注销 ─┼─> DDC 直连 gRPC 目标 ─> DDC Admin ─> Redis
Consumer ── 发现 / 订阅 Gateway 或 Provider ───────────────────────────────┘
```

DDC 目标是本地引导配置，DDC 不通过自身被发现。Provider 和 Gateway 注册信息是 Redis
中的临时租约；每次注册都有 `instanceId + leaseId` 身份，心跳与注销必须匹配完整租约身份。

### 职责边界

| 层次          | 本组件负责                                                              | 本组件不负责                          |
|-------------|--------------------------------------------------------------------|---------------------------------|
| Contract    | Java 绑定、Descriptor 校验、unary Contract Snapshot                      | 第二套 IDL 或序列化协议                  |
| Provider    | gRPC Server、Bean 分发、可用性、租约生命周期                                     | Gateway 路由或 Provider 健康探测       |
| Consumer    | Proxy、Channel 生命周期、Deadline、Metadata、Gateway/Provider Directory 接口、Consumer 候选节点负载均衡 | Gateway 规则、Gateway 侧 Provider 路由、业务重试策略 |
| DDC Adapter | 配置、注册、管理端口的直连 gRPC Client                                          | DDC Admin 持久化和 Redis 实现         |
| Gateway 集成  | 传输无关的 Gateway/Provider Directory Port 和 Contract Catalog           | 生产 Gateway 数据面和控制面              |

普通业务链路中，Consumer 只发现 `INTERNAL_GATEWAY`，不会查询 `RPC_PROVIDER`，也不会
创建 Provider Channel。直连 Provider 是显式的受控替代路径，仅由
`@EgonRpcDirectReference` 选择。

## Requirements

- JDK 21 或更高版本；Components Parent 强制要求 Java 21。
- 使用仓库提供的 Maven Wrapper（`./mvnw`），或兼容的 Maven 安装。
- 使用自动配置时，需要 Spring Boot 3.5.x。
- 使用 `protoc` 和 `protoc-gen-grpc-java` 生成兼容的 Java/gRPC 类；仓库当前基线为
  Protobuf 4.32.0 和 gRPC Java 1.75.0。
- 启用 Provider 租约、Gateway/Provider 发现或 DDC ConfigData 时，需要可访问的 DDC
  直连 RPC 端点和 Redis。
- 为启用的每项能力配置匹配的最小权限 DDC HMAC 凭据；本地开发可显式使用明文，生产应
  配置 mTLS。

## Quick Start

最短的 Spring Boot 使用方式是引入 DDC Adapter，准备一个 Protobuf Contract、一个
Provider 应用和一个经 Gateway 的 Consumer 应用。

1. 将 `.proto` 文件放在 `src/main/proto`，生成 Java/gRPC 源码。
2. 使用 `@EgonRpcService` 和 `@EgonRpcMethod` 声明 Java 接口。
3. 在 `@EgonRpcProvider` Spring Bean 上实现该接口。
4. 在 Consumer 中使用 `@EgonRpcReference` 注入该接口。
5. 开启 RPC、对应角色和 DDC Registry。若本地使用明文，必须同时显式打开 DDC 和业务
   RPC 的明文开关。

最小本地开发配置形状：

```yaml
spring:
  application:
    name: echo-provider

egon:
  cola:
    component:
      ddc:
        enabled: false
        biz-code: demo
        app-code: echo-provider
        env: dev
        namespace: default
        rpc:
          target: dns:///127.0.0.1:19080
          load-balancing-policy: round_robin
          tls:
            development-plaintext: true
          auth:
            registry:
              access-key: ${DDC_REGISTRY_ACCESS_KEY}
              secret-key: ${DDC_REGISTRY_SECRET_KEY}
        redis:
          host: 127.0.0.1
          port: 6379
        registry:
          enabled: true
      rpc:
        enabled: true
        tls:
          development-plaintext: true
        provider:
          enabled: true
          port: 19090
          advertised-host: 127.0.0.1
```

Consumer 使用同一 DDC Registry 作用域，将角色配置替换为：

```yaml
egon:
  cola:
    component:
      rpc:
        enabled: true
        tls:
          development-plaintext: true
        consumer:
          enabled: true
          gateway-service-name: egon-gateway-rpc
          gateway-group: default
          gateway-version: 1.0.0
```

DDC Admin、Redis、生产 Gateway 和 Gateway 规则不属于本组件的 Quick Start。完整多进程拓扑
请参阅 [Gateway、DDC 与 RPC 联调 Runbook](../../egon-cola-platforms/egon-cola-platform-gateway/docs/developer-integration.zh-CN.md)。

## Maven Dependency

导入 Components BOM，统一 Starter 和可选 DDC Adapter 的版本：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-components-bom</artifactId>
            <version>5.3.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

自行提供 Directory 或 Channel 策略时只依赖传输无关 Starter：

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-rpc-starter</artifactId>
</dependency>
```

使用 DDC ConfigData、基于 Registry 的 Provider/Gateway 发现或 DDC 管理 RPC 时依赖
DDC Adapter；它会传递引入 Starter 和 DDC SDK：

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-rpc-ddc-adapter</artifactId>
</dependency>
```

`egon-cola-component-rpc-test` 聚合模块及其 Contract/Provider/Consumer 应用仅用于测试，
不会由 BOM 导出。

## Configuration

### RPC 自动配置

只有 `egon.cola.component.rpc.enabled=true` 时，Starter 才导入
`EgonRpcAutoConfig`。Provider 与 Consumer 通过各自的 `provider.enabled` 和
`consumer.enabled` 独立创建。DDC Adapter 导入 `DdcRpcAutoConfiguration`，仅在对应 DDC
能力开启时暴露 DDC Client。

### Provider 配置

下表配置均位于 `egon.cola.component.rpc` 下。

| 配置项                                     |        默认值 | 说明                                       |
|-----------------------------------------|-----------:|------------------------------------------|
| `enabled`                               |    `false` | 启用 RPC 自动配置。                             |
| `provider.enabled`                      |    `false` | 启用 Provider 扫描、Server 和生命周期。             |
| `provider.bind-address`                 |  `0.0.0.0` | gRPC Server 绑定地址。                        |
| `provider.port`                         |    `19090` | gRPC Server 端口；测试可用 `0`。                 |
| `provider.advertised-host`              |    本机 Host | 发布到 DDC 的可路由 Host。                       |
| `provider.advertised-port`              |     实际绑定端口 | 发布到 DDC 的可路由端口。                          |
| `provider.registration-fail-fast`       |     `true` | 必需的初始注册失败时阻止启动。                          |
| `provider.registration-mode`            | `REQUIRED` | `REQUIRED` 发布租约；`DISABLED` 仅启动本地 Server。 |
| `provider.lease-seconds`                |       `30` | Provider 租约 TTL。                         |
| `provider.heartbeat-interval-seconds`   |       `10` | 心跳间隔，必须小于租约 TTL。                         |
| `provider.graceful-shutdown-timeout-ms` |    `10000` | Server 排空超时。                             |
| `provider.metadata`                     |          空 | 业务元数据；不能使用框架保留前缀。                        |
| `provider.metadata.gateway.weight`      | Contract `weight` 或 `100` | 上报实例容量，范围 `1..10000`。 |

Provider 先启动 gRPC Server，将 Handler 置为不可用，再为每个 Service Identity 注册一份
DDC 租约，注册成功后才恢复对应 Handler 的可用状态。租约失效或心跳失败会先摘除可用性，
恢复拿到新租约后才重新提供服务。停止时依次关闭 Handler 和恢复、停止心跳、注销精确租约，
最后排空 Server。

### Consumer 配置

| 配置项                                     |                默认值 | 说明                             |
|-----------------------------------------|-------------------:|--------------------------------|
| `consumer.enabled`                      |            `false` | 启用 Consumer Proxy 和发现集成。       |
| `consumer.default-timeout-ms`           |             `3000` | 默认 unary Deadline 上限。          |
| `consumer.gateway-discovery-timeout-ms` |             `5000` | Gateway 发现和 Channel Ready 超时。  |
| `consumer.gateway-service-name`         | `egon-gateway-rpc` | 精确 Gateway 服务身份。               |
| `consumer.gateway-group`                |          `default` | 精确 Gateway 分组。                 |
| `consumer.gateway-version`              |            `1.0.0` | 精确 Gateway 版本。                 |
| `consumer.gateway-biz-code`             |                  空 | 可选的 DDC 业务作用域覆盖。               |
| `consumer.gateway-app-code`             |                  空 | 可选的 DDC 应用作用域覆盖。               |
| `consumer.channel-drain-timeout-ms`     |             `5000` | 替换 Provider Channel 的排空超时。     |
| `consumer.gateway-max-attempts`         |                `2` | 一次逻辑调用最多考虑的 Gateway Channel 数。 |
| `consumer.max-retries`                  |                `3` | 默认同模式可用性重试预算。                |
| `consumer.default-load-balance`         |       `ROUND_ROBIN` | Consumer 默认选择策略。                  |
| `consumer.consistent-hash-virtual-nodes`|              `160` | `CONSISTENT_HASH` 环密度。               |
| `consumer.generic-cache-max-entries`    |              `256` | 泛化目标缓存最大条目数。                  |
| `consumer.generic-cache-idle-timeout-ms`|           `600000` | 泛化目标空闲淘汰时间。                    |

`@EgonRpcReference` 和 `@EgonRpcDirectReference` 共享 timeout、retry、load-balance、fallback
和 Hash resolver 公共字段。字段注入时就固定模式：Gateway 不可用时不会改走 Direct，Direct
失败时也不会进入 Gateway。只有获取失败和 Provider/Gateway 阶段 `UNAVAILABLE` 等可用性故障，
才会在同一模式候选内按负载均衡更换节点，直到总 Deadline 或重试预算耗尽。业务状态
`INVALID_ARGUMENT`、`PERMISSION_DENIED`、`NOT_FOUND`、`FAILED_PRECONDITION`、`ALREADY_EXISTS`、
`ABORTED` 以及业务异常都是终态，不重试。框架不推断幂等性；开启重试时必须由业务保证重复
安全（例如唯一单据编号配合覆盖/upsert）。Channel 会显式关闭 gRPC 自带的 Transport Retry。

### 身份与传输安全

| 配置项                                     | 说明                                 |
|-----------------------------------------|------------------------------------|
| `identity.env`                          | 未由 DDC Adapter 提供身份时使用的进程环境。       |
| `identity.host`                         | 对外发布的进程 Host；默认使用本机地址。             |
| `identity.instance-id`                  | 稳定的进程实例 ID；默认由应用、Host 和 PID 组合。    |
| `tls.enabled`                           | 启用业务 RPC mTLS。                     |
| `tls.development-plaintext`             | `tls.enabled=false` 时必须显式打开；仅用于开发。 |
| `tls.certificate-chain-path`            | Client 证书链和 Provider Server 证书路径。  |
| `tls.private-key-path`                  | Client 私钥和 Provider Server 私钥路径。   |
| `tls.trust-certificate-collection-path` | Trust Collection 路径。               |

`tls.enabled=true` 时，三个证书路径都必须是可读文件。Provider 注册信息会发布 Endpoint
是否安全。DDC 传输使用独立的 `egon.cola.component.ddc.rpc` 配置命名空间，并按
`runtime`、`registry`、`management` 能力使用不同 HMAC 凭据。完整 DDC 配置请参阅
[DDC README](../../egon-cola-platforms/egon-cola-platform-dynamic-config-center/README.zh-CN.md)。

### DDC RPC 配置

DDC Adapter 使用本地配置的直连 gRPC 目标：

| `egon.cola.component.ddc.rpc` 配置项 |           默认值 | 说明                                |
|-----------------------------------|--------------:|-----------------------------------|
| `target`                          |             无 | 必填目标，例如 `dns:///127.0.0.1:19080`。 |
| `connect-timeout`                 |          `3s` | DDC Channel 连接超时。                 |
| `default-timeout`                 |         `10s` | DDC 调用默认超时。                       |
| `load-balancing-policy`           | `round_robin` | gRPC Target 负载均衡策略。               |
| `max-inbound-message-size`        |     `4194304` | 最大入站消息字节数。                        |
| `shutdown-timeout`                |          `5s` | DDC Channel 关闭等待时间。               |
| `tls.development-plaintext`       |        `true` | DDC RPC 本地开发明文默认值。                |
| `auth.enabled`                    |        `true` | 启用 DDC RPC HMAC Metadata。         |

DDC Registry 路径还需要 `egon.cola.component.ddc.registry.enabled=true`、基于
Redisson 的 DDC Redis Client 和 Registry 凭据。DDC ConfigData 生命周期相互独立：
`ddc.enabled=true` 启用配置客户端；不启用远程配置时也可以单独启用 Registry 租约。

## Usage

### 1. 定义 Protobuf Contract

线协议放在 `src/main/proto`：

```proto
syntax = "proto3";

package example.echo.v1;

option java_multiple_files = true;
option java_package = "com.example.echo.proto";

service EchoService {
  rpc Echo(EchoRequest) returns (EchoResponse);
}

message EchoRequest {
  string message = 1;
}

message EchoResponse {
  string provider_id = 1;
  string message = 2;
}
```

项目父 POM 已管理 `protobuf-maven-plugin`、`protoc` 和 `protoc-gen-grpc-java`。Contract
模块需要执行标准的 `compile` 与 `compile-custom`；完整插件配置可参考测试 Contract 模块。

### 2. 将生成的 Service 绑定到 Java

```java
@EgonRpcService(
        grpcClass = EchoServiceGrpc.class,
        group = "default",
        version = "1.0.0"
)
public interface EchoRpc {

    @EgonRpcMethod(name = "Echo")
    EchoResponse echo(EchoRequest request);
}
```

生成的 Proto Service Name、`group` 和 `version` 共同构成 Service Identity。Validator
要求恰好一个请求参数、一个 Protobuf 响应、存在输入输出 Descriptor 匹配的生成方法，且
必须是 unary 非 streaming。Java 方法名不允许重载。Contract 无效时会在启动或创建 Proxy
时以 `RPC_INVALID_CONTRACT` 失败。

### 3. 暴露 Provider

```java
@EgonRpcProvider
public class EchoRpcProvider implements EchoRpc {

    @Override
    public EchoResponse echo(EchoRequest request) {
        return EchoResponse.newBuilder()
                .setProviderId("provider-a")
                .setMessage(request.getMessage())
                .build();
    }
}
```

`@EgonRpcProvider` 是 Spring Component 标记。Bean 至少需要实现一个
`@EgonRpcService` 接口。启用 DDC Registry 集成后，Provider 会和业务元数据一起上报
`transport=grpc`、`serialization=protobuf` 以及经过资源过滤的 Starter 运行时版本。

### 4. 经 Gateway 调用

```java
@Component
public class EchoClient {

    @EgonRpcReference(timeoutMs = 2000)
    private EchoRpc echoRpc;

    public EchoResponse echo(String message) {
        return echoRpc.echo(EchoRequest.newBuilder()
                .setMessage(message)
                .build());
    }
}
```

该路径需要 `RpcGatewayDirectory` 实现，通常由 DDC Adapter 提供。Consumer 按精确的
Gateway Service/Group/Version 订阅快照，为每个有效 Gateway 保持一个 gRPC Channel，并在
这些 Channel 之间选择调用目标。

### 5. 显式调用发现到的 Provider

适用于明确绕过 Gateway 规则的受信任内部调用：

```java
@Component
public class InternalEchoClient {

    @EgonRpcDirectReference(
            bizCode = "demo",
            appCode = "echo-provider",
            env = "dev"
    )
    private EchoRpc echoRpc;
}
```

该路径需要 `RpcProviderDirectory` 实现，通常由 DDC Adapter 提供。它发现 `RPC_PROVIDER`
条目并管理有效 Provider 的 Channel；它不执行 Gateway 路由、鉴权或流量治理。

### 6. 创建基础设施直连 Client

`RpcDirectClientFactory` 为一个显式配置的 gRPC 目标创建类型化 Proxy，例如 DDC 端口。
返回的 `RpcDirectClientHandle` 持有该 Channel，调用方必须负责关闭。它和
`@EgonRpcDirectReference` 不同：后者是从 DDC 发现业务 Provider。

## Core Concepts

### Contract 与 Descriptor Identity

Protobuf Descriptor 是线协议 Service、Method、Request、Response 以及 Streaming 形态的
事实来源。Java 注解负责绑定以及逻辑 `group`/`version`，不定义第二套序列化协议。组件可
生成包含依赖文件的 `FileDescriptorSet` 和 SHA-256 摘要，供 Gateway Reporting 和兼容性
检查使用。

### Provider 生命周期与租约

```text
扫描 @EgonRpcProvider
  -> 校验 Contract、构建 Handler
  -> 启动 gRPC Server
  -> 每个 Service Identity 注册一份租约
  -> 标记 Handler 可用
  -> 心跳并恢复失效租约
  -> 停止时禁用、注销精确租约并排空 Server
```

如果必需 Registry 在初始注册时不可用，Fail-Fast 会阻止 Provider 发布一个并未 Ready 的
服务。心跳失败后会先撤销可用性，再尝试租约恢复。

### Consumer Channel 模式

| 模式          | 入口                        | 发现对象               | Channel 所有者  | 重试边界                                       |
|-------------|---------------------------|--------------------|--------------|--------------------------------------------|
| Gateway     | `@EgonRpcReference`       | `INTERNAL_GATEWAY` | RPC Consumer | 仅同模式获取失败/`UNAVAILABLE` 换候选节点 |
| 直连 Provider | `@EgonRpcDirectReference` | `RPC_PROVIDER`     | RPC Consumer | 仅同模式获取失败/`UNAVAILABLE` 换候选节点 |
| 显式目标        | `RpcDirectClientFactory`  | 无                  | 调用方 Handle   | 一次传输尝试                                     |

普通 Consumer 链路不会直接发现 Provider。Gateway 的 Provider 选择、健康探测、路由规则和
Provider 负载均衡都属于 Gateway 平台。

Consumer 会为每个精确发现查询维护一份不可变本地 Snapshot。DDC Adapter 负责事件监听和
周期性全量对账；调用热路径只读本地 Snapshot，不在每次调用时拉 DDC。`RpcLoadBalancers`
提供 `RANDOM`、`WEIGHTED_RANDOM`、`ROUND_ROBIN`、`SMOOTH_WEIGHTED_ROUND_ROBIN`、
`CONSISTENT_HASH` 和 `LEAST_IN_FLIGHT`。权重来自 Provider 的 `gateway.weight` Metadata。
一致性 Hash 的 typed 调用必须配置命名 `RpcLoadBalanceKeyResolver`，泛化调用必须提供有界
`affinityKey`。

### 阻塞、异步、泛化与多路复用

Typed 方法可以返回 Protobuf 响应（阻塞调用），也可以返回
`CompletionStage<ProtobufResponse>`（异步调用）。两者复用同一 unary gRPC Descriptor 及
Metadata/Interceptor 链路。泛化 API 仅允许受限的 raw bytes：

```java
RpcGenericInvocation call = RpcGenericInvocation.gateway(
        "egon.rpc.test.v1.EchoService", "default", "1.0.0",
        "egon.rpc.test.v1.EchoService/Echo", requestBytes,
        3000, 1, LoadBalance.ROUND_ROBIN, FailStrategy.FAIL_CLOSED, null);
byte[] response = genericInvoker.invokeBlocking(call);
CompletionStage<byte[]> async = genericInvoker.invokeAsync(call);
```

唯一允许的方法身份是 gRPC canonical `fully.qualified.Service/Method`；点号别名、任意
Metadata、Endpoint 地址、DDC 凭据、Map/Object 序列化、Streaming 和第二套泛化 wire Service
都会被拒绝。泛化目标状态受缓存上限约束；同一 Endpoint Key 的并发 unary stream 共享一个
`ManagedChannel`，关闭时先排空再强制关闭。

### Provider Guard 限流

RPC 不定义第二套限流器，也不向 `@EgonRpcProvider` 增加字段。Provider 应显式依赖 Access
Guard Starter，并将已有注解放在实现方法上：

```java
@EgonRpcProvider
final class OrderProvider implements OrderRpc {
    @RateLimitGuard("rpc.order.create")
    public CompletionStage<OrderResponse> create(OrderRequest request) { ... }
}
```

Guard Rule 在 `TOKEN_BUCKET`、`LEAKY_BUCKET`、`SLIDING_WINDOW` 中选择算法。算法 Strategy/
Factory、Local/Redisson 后端负责 capacity、Key 作用域、原子性和 failure policy。`RATE_LIMITED`
会由 RPC Adapter 映射为 gRPC `UNAVAILABLE`，携带 Provider-stage 与 `error-type=rate-limit`
Trailer，业务方法不会执行；其他 Guard 决策仍走原有拒绝链路。缺少 Guard Starter 时，RPC
可选 Adapter 不创建，也不会静默假设存在限流。

### 生命周期状态与优雅关闭

Provider 状态为 `NEW → STARTING → READY|DEGRADED → DRAINING → STOPPED`（也可能进入
`FAILED`）。只有 Server 已绑定且所有必需租约 active 后才进入 READY。Provider 心跳由 RPC
侧 fixed-delay 调度；DDC 只负责租约校验、续期、过期和变更事件，不主动探测 Provider。Consumer
启动先安装所有声明的 Directory 订阅与共享 Channel Pool，再接受调用。关闭先关准入门、停止
订阅/恢复、注销精确租约，在配置的超时内排空 in-flight unary，最后强制关闭剩余 Channel；
`SmartLifecycle` stop callback 只执行一次。

`FAIL_OPEN` 是显式的降级结果合同：可用性尝试耗尽时返回 `null`（typed 或 generic），调用方
必须处理；需要业务结果的调用不应依赖它，应使用本地 fallback 或显式 null 检查。

### Deadline、Metadata 与 Status

Proxy 的有效 Deadline 是 Reference Timeout 与 Consumer 默认值两者中的较小值。调用沿用
标准 gRPC Deadline 与 Cancellation 语义。内置 Client Interceptor 会发送长度受控的 ASCII
Metadata，包括 Service Identity、调用/来源 Identity、W3C Trace Context 和 Request Identity。
Provider 业务代码可通过 `RpcInvocationMetadata.current()` 读取已校验的调用信息。

主要 Status 映射如下：

| gRPC Status/标记                                          | `EgonRpcErrorCode`         |
|---------------------------------------------------------|----------------------------|
| `DEADLINE_EXCEEDED`                                     | `RPC_DEADLINE_EXCEEDED`    |
| `CANCELLED`                                             | `RPC_CANCELLED`            |
| 未携带 Provider 标记的 `UNAVAILABLE`                          | `RPC_GATEWAY_UNAVAILABLE`  |
| 携带 `x-egon-rpc-failure-stage: provider` 的 `UNAVAILABLE` | `RPC_PROVIDER_UNAVAILABLE` |
| `INVALID_ARGUMENT`                                      | `RPC_INVALID_REQUEST`      |
| `PERMISSION_DENIED`                                     | `RPC_PROVIDER_REJECTED`    |
| `UNIMPLEMENTED` 或 Method Not Found 标记                   | `RPC_METHOD_NOT_FOUND`     |
| 未携带 Method 标记的 `NOT_FOUND`                              | `RPC_SERVICE_NOT_FOUND`    |

Provider Exception Mapper 和 Gateway 转发下游失败时，必须保留 failure-stage Trailer。

### Gateway 与 DDC 边界

DDC Adapter 负责三个能力域的直连 DDC RPC Client：ConfigData Runtime、Service Registry 和
Management。DDC Registry 提供临时 Provider/Gateway 租约及实时快照。Gateway 平台通过自身
Starter 消费 RPC Contract Catalog/Snapshot，并负责生产数据面。路由、规则、健康、安全和
流量治理请参阅 [Gateway README](../../egon-cola-platforms/egon-cola-platform-gateway/README.zh-CN.md)。

## Extension Points

Starter 将发现和注册封装为 Port，业务应用不需要依赖 DDC 实现类型：

| 扩展点                              | 用途                                             |
|----------------------------------|------------------------------------------------|
| `RpcProviderRegistry`            | 提供 Provider 租约注册、心跳和注销。                        |
| `RpcGatewayDirectory`            | 为 Gateway 模式 Consumer 提供实时 Gateway Snapshot。   |
| `RpcProviderDirectory`           | 为直连 Reference Consumer 提供实时 Provider Snapshot。 |
| `RpcClientInterceptorFactory`    | 添加按请求创建的有序 gRPC Client Interceptor。            |
| `RpcProviderExceptionMapper`     | 将 Provider 领域异常映射为 gRPC Status 和 Trailer。      |
| `RpcProviderMetadataContributor` | 为 Service Identity 增加注册 Metadata。              |
| `RpcInvocationChannelProvider`   | 为 Proxy Factory 提供自定义 Channel 选择和生命周期策略。       |
| `RpcProcessIdentityProvider`     | 提供应用、环境、Host 和实例身份。                            |
| `RpcContractCatalog`             | 替换或适配供集成方使用的已校验 Contract Catalog。              |
| Spring `ServerInterceptor` Bean  | 增加 Provider 侧 gRPC Server Interceptor。         |

生产 Gateway 的路由、鉴权、负载均衡、熔断和限流扩展点不属于本模块，应在 Gateway 平台实现。

## Project Structure

```text
egon-cola-component-rpc/
├── pom.xml
├── README.md
├── README.zh-CN.md
├── egon-cola-component-rpc-starter/
│   └── src/main/java/top/egon/cola/component/rpc/
│       ├── annotation/       # Contract、Provider 和 Reference 注解
│       ├── config/            # Spring Boot 配置与自动配置
│       ├── contract/          # Descriptor、校验、Catalog 和 Snapshot
│       ├── consumer/          # Proxy、Directory、Channel 和 Interceptor
│       ├── context/            # 进程身份与调用 Metadata
│       ├── exception/          # 稳定 RPC 异常与 Status 映射
│       └── provider/           # Binding、Server、可用性与租约
├── egon-cola-component-rpc-ddc-adapter/
│   └── src/main/java/top/egon/cola/component/rpc/ddc/
│       ├── autoconfigure/      # DDC RPC 配置与 Spring 装配
│       ├── client/             # Config、Registry 和 Management Client
│       ├── contract/            # 面向 DDC Protobuf 的 Java Contract
│       ├── mapping/             # Protobuf/领域 Mapper 与 Status 映射
│       ├── registry/            # DDC RPC Directory 和 Registry Port
│       └── security/            # HMAC 规范化与 Metadata 签名
└── egon-cola-component-rpc-test/
    ├── ...-test-contract/      # Echo Proto 和生成的 Contract
    ├── ...-test-provider/      # Provider 进程 Fixture
    └── ...-test-consumer/      # Consumer 进程 Fixture 与 Mock Gateway 测试
```

## Compatibility

- **Java：** 21+。
- **Spring Boot：** 3.5.x；当前父 POM 管理 3.5.16。
- **gRPC/Protobuf：** gRPC Java 1.75.0、Protobuf Java/protoc 4.32.0、
  `protoc-gen-grpc-java` 1.75.0 是仓库兼容基线。
- **线协议：** V1 只接受生成的 Protobuf `Message` 请求/响应类型，以及 unary、非 streaming
  gRPC 方法。
- **发现协议：** DDC 发现依赖当前 DDC Registry Service Identity 和租约模型；自定义 Registry
  应实现 Starter Port，不要复制 DDC 内部实现。
- **Schema 演进：** 保持生成的 Service/Method Name 与 Protobuf 字段兼容；Descriptor
  SHA-256 变化会影响 Gateway 接口上报和兼容性检查。
- **安全：** 明文必须显式处于开发模式；生产 Endpoint 应使用 mTLS，并为 DDC 各能力使用
  独立 HMAC 凭据。

## Roadmap

以下内容不属于当前 V1 Runtime Contract，实施前需要单独完成 Contract/Design 决策：

- Streaming RPC 及其 Gateway Descriptor/Reporting 模型。
- 生产级 Metrics 面板和故障演练自动化；运行时已有有界诊断及 Status/Trailer 合同。
- 依赖具体环境的 DDC/Redis 拓扑与 mTLS 校验；它们属于部署侧证据，不由模块单测替代。

## Validation

在仓库根目录运行普通 RPC 模块测试：

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract \
  -am test
```

普通测试使用真实 loopback TCP、测试 Mock Gateway 和 Direct Provider Fixture，不能证明生产 DDC/Redis/Gateway 拓扑。
可选进程测试需要外部管理的 Redis：

```bash
DDC_TEST_REDIS_HOST=127.0.0.1 \
DDC_TEST_REDIS_PORT=6379 \
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract \
  -am -Pddc-live-test -Dit.test=RpcProcessIT verify
```
