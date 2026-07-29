# Egon COLA RPC

[English](README.md) | [中文](README.zh-CN.md)

## 范围

`egon-cola-component-rpc` 是基于标准 gRPC Java 和 Protobuf 的轻量级
Spring Boot RPC 框架。Provider 暴露带注解的 Java Bean，并将生成的 Proto
服务身份注册到动态配置中心（DDC）；Consumer 创建 JDK Proxy，发现有效的内部
Gateway，为每个 Gateway 保持一个通道，并在总 Deadline 内将调用分布到这些通道。

生产 Gateway 由同级的 [Gateway 组件](../egon-cola-component-gateway/README.md) 实现。
本组件负责 Provider/Consumer 契约和生命周期集成，不内嵌 Gateway 数据面。测试源码
仍保留 Mock Gateway，用于隔离验证 Consumer → Gateway → Provider 链路。

V1 只支持请求和响应均实现 Protobuf `Message` 的 unary 方法。`.proto` 是唯一
IDL。Java 注解只负责将生成的 Descriptor 绑定到 Java 接口，不重复定义服务、
消息或序列化协议。

## 模块

| 模块 | 职责 | 是否发布 |
|---|---|---|
| `egon-cola-component-rpc-starter` | Provider Server、Consumer Proxy、DDC 注册发现、Metadata、Deadline 和异常转换 | 是，并加入 Components BOM |
| `egon-cola-component-rpc-test` | 仅测试使用的聚合模块 | 否 |
| `...-test-contract` | 单个 Echo Proto 及生成的 Java/gRPC 类 | 否 |
| `...-test-provider` | Provider 测试应用 | 否 |
| `...-test-consumer` | Consumer 测试应用 | 否 |
| `...-test-suite` | Mock Gateway、真实 TCP 测试和可选进程验证 | 否 |

RPC 根模块有意只包含 Starter 与 Test 两个聚合模块。所有测试产物均不进入公共
BOM。

## 运行拓扑

```text
Consumer ── 每个 Gateway 一个 Channel ──> 内部 Gateway 集合
                                             │
                                             ├── 发现 RPC_PROVIDER 服务组
                                             ├── 选择 Provider 实例
                                             └── 转发 unary 调用 ──> Provider

Provider ── 注册/心跳/注销 ─┐
Gateway  ── 注册/心跳/注销 ─┼──> DDC Admin 集合 ──> 共享 Redis
Consumer ── 发现/订阅 Gateway ─┘
```

DDC 提供共享的租约和服务注册边界。Provider 与 Gateway 注册信息都是 Redis 中的
临时租约。每次注册生成新的 `leaseId`；心跳和注销原子匹配 `instanceId + leaseId`。

Consumer 永不查询 `RPC_PROVIDER`，也不创建 Provider Channel。它在有效 Gateway
Channel 间 Round Robin；只有 Gateway 阶段的 `UNAVAILABLE` 才会在同一个调用
Deadline 内尝试其他 Gateway。启动截止时间内没有活跃 Gateway，或所有 Gateway
都丢失时，Consumer 会快速失败；Provider 阶段失败不会由 Consumer 重试。

## 依赖

生产应用导入 Components BOM，并且只依赖 Starter：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-components-bom</artifactId>
            <version>5.2.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-component-rpc-starter</artifactId>
    </dependency>
</dependencies>
```

## Proto 代码生成

Contract 放在 `src/main/proto`，使用标准 `protoc` 和
`protoc-gen-grpc-java`。项目父 POM 已管理下列插件与工具版本：

```xml
<build>
    <extensions>
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>${os-maven-plugin.version}</version>
        </extension>
    </extensions>
    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <configuration>
                <protocArtifact>
                    com.google.protobuf:protoc:${protoc.version}:exe:${os.detected.classifier}
                </protocArtifact>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>
                    io.grpc:protoc-gen-grpc-java:${protoc-gen-grpc-java.version}:exe:${os.detected.classifier}
                </pluginArtifact>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>compile-custom</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

IDL 示例：

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

## Java Contract

将生成的 gRPC Service 绑定到 Java 接口：

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

Provider 和 Consumer 启动时执行严格校验：

- 必须是至少包含一个方法的注解接口；
- Java 方法不能重载；
- 必须只有一个请求参数和一个响应值；
- 请求和响应必须是生成的 Protobuf `Message`；
- 必须存在输入、输出 Descriptor 完全一致的生成 gRPC 方法；
- 只允许 unary、非 streaming 语义。

任何不一致都会以 `RPC_INVALID_CONTRACT` 阻止启动。

## Provider

由 Spring Bean 实现 Contract：

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

配置 DDC 注册中心和 Provider：

```yaml
spring:
  application:
    name: echo-provider

egon:
  cola:
    component:
      ddc:
        enabled: false
        env: dev
        namespace: default
        admin:
          endpoint: http://127.0.0.1:18080
          signature-enabled: false
        redis:
          host: 127.0.0.1
          port: 6379
        registry:
          enabled: true
      rpc:
        enabled: true
        provider:
          enabled: true
          bind-address: 0.0.0.0
          port: 19090
          advertised-host: 127.0.0.1
          lease-seconds: 30
          heartbeat-interval-seconds: 10
          registration-fail-fast: true
          graceful-shutdown-timeout-ms: 10000
```

Provider 配置项：

| `egon.cola.component.rpc` 下的配置 | 默认值 | 含义 |
|---|---:|---|
| `enabled` | `false` | 启用 RPC 自动配置 |
| `provider.enabled` | `false` | 启用 Provider 扫描、Server 和租约 |
| `provider.bind-address` | `0.0.0.0` | gRPC Server 绑定地址 |
| `provider.port` | `19090` | gRPC Server 端口；测试可用 `0` 选择空闲端口 |
| `provider.advertised-host` | 进程 Host | 注册到 DDC 的可路由 Host |
| `provider.advertised-port` | 实际绑定端口 | 注册到 DDC 的可路由端口 |
| `provider.registration-fail-fast` | `true` | 初始注册失败时阻止启动 |
| `provider.lease-seconds` | `30` | Provider 租约 TTL |
| `provider.heartbeat-interval-seconds` | `10` | Provider 心跳间隔 |
| `provider.graceful-shutdown-timeout-ms` | `10000` | Server 排空超时 |
| `provider.metadata` | 空 | 不含保留前缀的业务元数据 |

`ddc.enabled: false` 只关闭配置客户端生命周期，注册中心可独立保持启用。业务
应用同时需要动态配置时，可将其设为 `true`。

Provider 先启动 gRPC Server，再为每个暴露服务注册一份 DDC 租约，成功后才将
Handler 标记为可用。心跳失败或租约不匹配时，Handler 在取得新租约前保持不可用。
停止时先将 Handler 与租约恢复置为不可用，再停止心跳、主动注销精确租约，并优雅
排空 Server，超时后才强制终止。

可在 `provider.metadata` 下配置 Provider 元数据，但业务项不得使用
`ddc.`、`egon.internal.` 或 `egon.rpc.` 前缀。transport、serialization 和
runtime-version 元数据由框架写入。运行时版本默认从 Starter 的过滤资源中读取；
`egon.rpc.runtime-version` 只作为显式覆盖项。

## Consumer

将 Contract 注入 Spring Bean：

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

配置 Consumer 以及精确的 Gateway 服务身份：

```yaml
spring:
  application:
    name: echo-consumer

egon:
  cola:
    component:
      ddc:
        enabled: false
        env: dev
        namespace: default
        admin:
          endpoint: http://127.0.0.1:18080
        redis:
          host: 127.0.0.1
          port: 6379
        registry:
          enabled: true
      rpc:
        enabled: true
        consumer:
          enabled: true
          default-timeout-ms: 3000
          gateway-discovery-timeout-ms: 5000
          gateway-service-name: egon-gateway-rpc
          gateway-group: default
          gateway-version: 1.0.0
          channel-drain-timeout-ms: 5000
          gateway-max-attempts: 2
```

Consumer 配置项：

| `egon.cola.component.rpc` 下的配置 | 默认值 | 含义 |
|---|---:|---|
| `enabled` | `false` | 启用 RPC 自动配置 |
| `consumer.enabled` | `false` | 启用 Gateway 发现与 Consumer Proxy |
| `consumer.default-timeout-ms` | `3000` | 默认 unary Deadline 上限 |
| `consumer.gateway-discovery-timeout-ms` | `5000` | 启动发现及 Channel Ready 超时 |
| `consumer.gateway-service-name` | `egon-gateway-rpc` | 精确 Gateway 服务名 |
| `consumer.gateway-group` | `default` | 精确 Gateway 分组 |
| `consumer.gateway-version` | `1.0.0` | 精确 Gateway 版本 |
| `consumer.channel-drain-timeout-ms` | `5000` | 被替换 Channel 的排空超时 |
| `consumer.gateway-max-attempts` | `2` | 一次调用最多尝试的 Gateway Channel 数 |

`@EgonRpcReference.timeoutMs` 可以缩短但不能超过
`default-timeout-ms`。所有生产 Consumer Channel 都显式调用 gRPC
`disableRetry()`。

## Metadata、状态与取消

Consumer 发送长度受控的 ASCII Metadata，包括 service、group、version、
invocation ID、来源应用、来源实例、`traceparent`、`tracestate` 和
`x-egon-request-id`。Gateway 负责转发标准 Trace Context，Provider 通过
`RpcInvocationMetadata.current()` 向业务代码暴露校验后的 traceId、requestId、
spanId、parentSpanId 和 invocationId。`x-egon-trace-id` 不再作为传播字段写出；
旧字段只保留常量兼容测试和安全忽略路径。非法或超长值会被丢弃，不传入业务代码。

gRPC Status 会转换为稳定的框架错误：

| gRPC 状态 | RPC 错误 |
|---|---|
| `DEADLINE_EXCEEDED` | `RPC_DEADLINE_EXCEEDED` |
| `CANCELLED` | `RPC_CANCELLED` |
| 未携带下游标记的 `UNAVAILABLE` | `RPC_GATEWAY_UNAVAILABLE` |
| 携带 `x-egon-rpc-failure-stage: provider` 的 `UNAVAILABLE` | `RPC_PROVIDER_UNAVAILABLE` |
| `INVALID_ARGUMENT` | `RPC_INVALID_REQUEST` |
| `PERMISSION_DENIED` | `RPC_PROVIDER_REJECTED` |
| `NOT_FOUND` | `RPC_SERVICE_NOT_FOUND` 或 `RPC_METHOD_NOT_FOUND` |
| 其他状态 | `RPC_INTERNAL` |

Deadline 和 Cancellation 复用标准 gRPC 传播语义。V1 不新增框架重试，也不会
重试业务调用。Gateway 转发下游 Provider 的 `UNAVAILABLE` 时，必须增加框架
failure-stage Trailer。

## Gateway 边界

Starter 不打包生产 Gateway。Gateway 组件负责 Gateway 注册、Provider Directory、
规则执行、HTTP/RPC 转发、健康检查和流量治理。下列能力仍是
`rpc-test-suite/src/test` 下的测试夹具：

- Mock Gateway 注册和心跳；
- Provider 服务目录与实例 Directory；
- Provider Channel 缓存；
- 动态字节级 Handler Registry；
- 确定性的测试轮询 Selector；
- unary Forwarder。

生产数据面和控制面的边界请参阅 Gateway README。

## 测试

普通测试使用真实 loopback TCP 验证
Consumer → Mock Gateway → Provider，不使用 gRPC In-Process Transport：

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite \
  -am test
```

套件同时覆盖 Cancellation，以及多 Provider 的 Directory
发现、租约替换和摘除。Gateway 选择和 Provider 负载均衡属于 Gateway 组件；
Consumer 只负责在有效 Gateway Channel 间选择。

可选 `verify` Profile 会在四个独立 JVM 中启动一个 Admin、一个 Provider、
一个 Mock Gateway 和一个 Consumer；它要求外部提供 Redis 单节点：

```bash
DDC_TEST_REDIS_HOST=127.0.0.1 \
DDC_TEST_REDIS_PORT=6379 \
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite \
  -am -Pddc-live-test -Dit.test=RpcProcessIT verify
```

Redis 需要鉴权时设置 `DDC_TEST_REDIS_PASSWORD`。进程测试使用临时 SQLite，
校验 Flyway V1/V2，将子进程日志写入 `target/rpc-process-it`，并按反向顺序
停止所有子进程。

## V1 非目标

生产 Gateway 拓扑、双 Engine 调用和故障演练见
[Gateway + DDC + RPC 联调 Runbook](../egon-cola-component-gateway/docs/developer-integration.zh-CN.md)。

V1 不实现：

- Consumer 直连 Provider；
- Consumer 侧负载均衡或 Provider 发现；
- 灰度/金丝雀路由；
- 限流或熔断；
- 业务重试；
- streaming RPC；
- 非 Protobuf 序列化；
- Consumer 侧 Provider 发现或直连 Provider Channel；
- Gateway 规则管理、HTTP 路由、Provider 负载均衡或流量治理；
- 第二种序列化协议或 streaming RPC。

这些边界使 Starter 只负责 Contract、服务暴露、代理、注册发现接入、Metadata、
超时、取消和异常语义，流量治理统一留给 Gateway。
