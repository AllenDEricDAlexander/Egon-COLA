# Egon COLA RPC

[English](README.md) | [中文](README.zh-CN.md)

## 范围

`egon-cola-component-rpc` 是基于标准 gRPC Java 和 Protobuf 的轻量级
Spring Boot RPC 框架。Provider 暴露带注解的 Java Bean，并将生成的 Proto
服务身份注册到动态配置中心（DDC）；Consumer 创建 JDK Proxy，只发现唯一的
内部 Gateway，并把所有调用统一发往 Gateway。

生产 Gateway 有意不放在本组件中。仓库仅在测试源码下提供 Mock Gateway，
用于在生产 Gateway 开发前验证完整的
Consumer → Gateway → Provider 调用链。

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
Consumer ── 单条 gRPC Channel ──> 唯一活跃内部 Gateway
                                      │
                                      ├── 发现 RPC_PROVIDER 服务组
                                      ├── 选择实例
                                      └── 转发 unary 调用 ──> Provider

Provider ── 注册/心跳/注销 ─┐
Gateway  ── 注册/心跳/注销 ─┼──> 单 DDC Admin ──> 单 Redis
Consumer ── 发现/订阅 Gateway ─┘
```

DDC V1 是单机拓扑：一个 Admin 和一个 Redis 单节点连接。Provider 与 Gateway
注册信息都是 Redis 临时租约。每次注册生成新的 `leaseId`；心跳和注销原子匹配
`instanceId + leaseId`。

Consumer 永不查询 `RPC_PROVIDER`，不创建 Provider Channel，也不包含负载均衡
算法。启动截止时间内没有活跃 Gateway、发现多个活跃 Gateway，或运行中唯一
Gateway 丢失时，Consumer 都会快速失败。

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
          gateway-service-name: egon-internal-rpc-gateway
          gateway-group: default
          gateway-version: 1.0.0
          channel-drain-timeout-ms: 5000
```

Consumer 配置项：

| `egon.cola.component.rpc` 下的配置 | 默认值 | 含义 |
|---|---:|---|
| `enabled` | `false` | 启用 RPC 自动配置 |
| `consumer.enabled` | `false` | 启用 Gateway 发现与 Consumer Proxy |
| `consumer.default-timeout-ms` | `3000` | 默认 unary Deadline 上限 |
| `consumer.gateway-discovery-timeout-ms` | `5000` | 启动发现及 Channel Ready 超时 |
| `consumer.gateway-service-name` | `egon-internal-rpc-gateway` | 精确 Gateway 服务名 |
| `consumer.gateway-group` | `default` | 精确 Gateway 分组 |
| `consumer.gateway-version` | `1.0.0` | 精确 Gateway 版本 |
| `consumer.channel-drain-timeout-ms` | `5000` | 被替换 Channel 的排空超时 |

`@EgonRpcReference.timeoutMs` 可以缩短但不能超过
`default-timeout-ms`。所有生产 Consumer Channel 都显式调用 gRPC
`disableRetry()`。

## Metadata、状态与取消

Consumer 发送长度受控的 ASCII Metadata，包括 service、group、version、
invocation ID、来源应用、来源实例和 trace ID。Gateway 负责原样转发，Provider
通过 `RpcInvocationMetadata.current()` 向业务代码暴露校验后的值。
`traceparent` 和 `tracestate` 也保留为 Gateway 转发及 Provider 接收字段；V1
Starter 自动生成或继承的是 trace ID。非法或超长值会被丢弃，不传入业务代码。

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

Starter 中不存在生产 Gateway package。下列能力只存在于
`rpc-test-suite/src/test`：

- Gateway 注册和心跳；
- Provider 服务目录与实例 Directory；
- Provider Channel 缓存；
- 动态字节级 Handler Registry；
- 确定性的测试轮询 Selector；
- unary Forwarder。

它们是参考测试夹具，不是可复用的生产 API。生产 Gateway 会在 DDC 与 RPC
Contract 验收后独立开发。

## 测试

普通测试使用真实 loopback TCP 验证
Consumer → Mock Gateway → Provider，不使用 gRPC In-Process Transport：

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite \
  -am test
```

套件同时覆盖 Cancellation，以及多 Provider 的 Directory
发现、租约替换和摘除。实例选择策略属于 Mock Gateway，不构成 Starter 能力。

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

V1 不实现：

- Consumer 直连 Provider；
- Consumer 侧负载均衡或 Provider 发现；
- 灰度/金丝雀路由；
- 限流或熔断；
- 业务重试；
- streaming RPC；
- 非 Protobuf 序列化；
- 生产 Gateway、Provider Directory、Gateway Handler Registry、Provider
  Channel Factory、实例 Selector 或 Unary Forwarder。

这些边界使 Starter 只负责 Contract、服务暴露、代理、注册发现接入、Metadata、
超时、取消和异常语义，流量治理统一留给 Gateway。
