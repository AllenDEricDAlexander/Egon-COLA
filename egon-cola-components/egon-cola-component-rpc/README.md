# Egon COLA RPC

[English](README.md) | [中文](README.zh-CN.md)

`egon-cola-component-rpc` is the RPC transport component for Egon COLA. It
binds generated Protobuf/gRPC services to Java interfaces, provides Spring Boot
Provider and Consumer lifecycles, and integrates service leases and discovery
through Tianshu (Dynamic Config Center).

The component is deliberately not a Yuheng data plane. The production
Yuheng, routing rules, provider health policy, traffic governance, and HTTP/RPC
forwarding are owned by the separate [Yuheng platform](../../egon-cola-xingyuan/egon-cola-yuheng/README.md).

## Badges

![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![gRPC 1.75](https://img.shields.io/badge/gRPC-1.75.0-244C5A)
![Protobuf 4.32](https://img.shields.io/badge/Protobuf-4.32.0-4285F4)

## Features

- Standard gRPC Java and Protobuf transport; the `.proto` contract remains the
  only wire IDL.
- Strict startup validation between Java methods, generated gRPC descriptors,
  and Protobuf request/response messages.
- Provider bean scanning with `@EgonRpcProvider`, a managed unary gRPC server,
  availability gating, Tianshu lease registration, heartbeat, recovery, and exact
  lease deregistration.
- Consumer CGLIB proxies with one reference annotation and two selectable runtime
  paths: `@EgonRpcReference` defaults to direct Provider discovery; set
  `mode = RpcReferenceMode.GATEWAY` to use a discovered internal Yuheng.
- Programmatic direct gRPC clients for infrastructure endpoints through
  `RpcDirectClientFactory`; the caller owns the returned channel handle.
- Blocking and `CompletionStage` invocation, restricted generic raw-Protobuf
  calls, HTTP/2 channel multiplexing, deadline/cancellation propagation, bounded trace/request metadata, and
  stable `EgonRpcErrorCode` mapping from gRPC status values.
- Optional Tianshu RPC adapter for ConfigData, service registry, and management
  clients, with explicit mTLS or development-plaintext configuration and
  capability-specific HMAC credentials.
- Protobuf descriptor snapshots that can be consumed by the Yuheng reporting
  starter without creating a second schema or serialization model.

## Architecture

### Runtime topology

```text
Business Consumer
  ├─ @EgonRpcReference(mode=GATEWAY) ──> active INTERNAL_GATEWAY set ──> RPC_PROVIDER ──> Provider
  └─ @EgonRpcReference(mode=DIRECT, bizCode/appCode) ──> Tianshu RPC registry ──> RPC_PROVIDER ──> Provider

Provider ── register / heartbeat / deregister ─┐
Yuheng  ── register / heartbeat / deregister ─┼─> direct Tianshu gRPC target ─> Tianshu Admin ─> Redis
Consumer ── discover / subscribe Yuheng or Provider ────────────────────────┘
```

The Tianshu target is local bootstrap configuration. Tianshu is not discovered through
itself. Provider and Yuheng registrations are temporary Redis leases; each
registration has an `instanceId + leaseId` identity, and heartbeat/deregistration
must match the complete lease identity.

### Responsibility boundary

| Layer               | Owned by this component                                                        | Not owned by this component                                   |
|---------------------|--------------------------------------------------------------------------------|---------------------------------------------------------------|
| Contract            | Java binding, descriptor validation, unary contract snapshot                   | A second IDL or serializer                                    |
| Provider            | gRPC server, bean dispatch, availability, lease lifecycle                      | Yuheng routing or provider health probing                    |
| Consumer            | Proxy, channel lifecycle, deadline, metadata, Yuheng/Provider directory ports, Consumer-side candidate load balancing | Yuheng rules, Yuheng-side Provider routing, business retry policy |
| Tianshu adapter         | Direct gRPC clients for config, registry, and management ports                 | Tianshu Admin persistence and Redis implementation                |
| Yuheng integration | Transport-neutral Yuheng/Provider directory interfaces and contract catalog   | Production Yuheng data plane and control plane               |

For the normal business path, the Consumer discovers only `INTERNAL_GATEWAY`.
It does not query `RPC_PROVIDER` or open Provider channels. The Yuheng path is
selected explicitly with `@EgonRpcReference(mode = RpcReferenceMode.GATEWAY)`;
when no mode is written, the reference defaults to direct Provider discovery.

## Requirements

- JDK 21 or later. The components parent enforces Java 21.
- Maven Wrapper from the repository (`./mvnw`) or a compatible Maven installation.
- Spring Boot 3.5.x when using the auto-configuration.
- Generated Java and gRPC classes produced by `protoc` and
  `protoc-gen-grpc-java` compatible with the versions managed by this repository:
  Protobuf 4.32.0 and gRPC Java 1.75.0.
- A reachable Tianshu direct RPC endpoint and Redis when Provider leases, Yuheng or
  Provider discovery, or Tianshu ConfigData is enabled.
- Matching least-privilege Tianshu HMAC credentials for each enabled capability;
  local development may explicitly use plaintext, but production deployments
  should configure mTLS.

## Quick Start

The shortest Spring Boot setup uses the Tianshu adapter, one Protobuf contract, one
Provider application, and one Consumer application. Choose the direct default
or opt into the Yuheng proxy at the reference annotation.

1. Put the `.proto` file in `src/main/proto` and generate Java/gRPC sources.
2. Declare a Java interface with `@EgonRpcService` and
   `@EgonRpcMethod`.
3. Implement the interface on an `@EgonRpcProvider` Spring bean.
4. Inject the interface with `@EgonRpcReference` in the Consumer; add
   `mode = RpcReferenceMode.GATEWAY` for Yuheng proxy calls, otherwise provide
   direct `bizCode` and `appCode`.
5. Enable RPC, the relevant role, and Tianshu registry access. For a local
   plaintext setup, set both Tianshu and business RPC plaintext switches explicitly.

Minimal local development shape:

```yaml
spring:
  application:
    name: echo-provider

egon:
  cola:
    component:
      tianshu:
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
              access-key: ${TIANSHU_REGISTRY_ACCESS_KEY}
              secret-key: ${TIANSHU_REGISTRY_SECRET_KEY}
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

For a Consumer, use the same Tianshu registry scope and replace the role block with:

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
          yuheng-service-name: egon-yuheng-rpc
          yuheng-group: default
          yuheng-version: 1.0.0
```

The Tianshu Admin, Redis, production Yuheng, and Yuheng rules are outside this
component's Quick Start. Use
the [Yuheng and Tianshu integration runbook](../../egon-cola-xingyuan/egon-cola-yuheng/docs/developer-integration.md)
for a complete multi-process topology.

## Maven Dependency

Import the Components BOM so the Starter and optional Tianshu Adapter use the
repository-managed versions:

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

Use the transport-neutral Starter when supplying your own directory or channel
strategy:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-rpc-starter</artifactId>
</dependency>
```

Use the Tianshu Adapter for Tianshu ConfigData, registry-backed Provider/Yuheng
discovery, or Tianshu management RPC. It brings the Starter and Tianshu SDK transitively:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-rpc-tianshu-adapter</artifactId>
</dependency>
```

The `egon-cola-component-rpc-test` aggregator and its contract/provider/consumer
applications are test-only modules and are not exported by the BOM.

## Configuration

### RPC auto-configuration

The Starter imports `EgonRpcAutoConfig` only when
`egon.cola.component.rpc.enabled=true`. Provider and Consumer beans are created
independently through their respective `provider.enabled` and `consumer.enabled`
flags. The Tianshu Adapter imports `DdcRpcAutoConfiguration` and exposes Tianshu clients
only when the corresponding Tianshu features are enabled.

### Provider properties

All properties in this table are under `egon.cola.component.rpc`.

| Property                                |    Default | Description                                                     |
|-----------------------------------------|-----------:|-----------------------------------------------------------------|
| `enabled`                               |    `false` | Enables RPC auto-configuration.                                 |
| `provider.enabled`                      |    `false` | Enables Provider scanning, server, and lifecycle.               |
| `provider.bind-address`                 |  `0.0.0.0` | gRPC server bind address.                                       |
| `provider.port`                         |    `19090` | gRPC server port; `0` is useful for tests.                      |
| `provider.advertised-host`              | local host | Routable host published to Tianshu.                                 |
| `provider.advertised-port`              | bound port | Routable port published to Tianshu.                                 |
| `provider.registration-fail-fast`       |     `true` | Fails startup when required initial registration fails.         |
| `provider.registration-mode`            | `REQUIRED` | `REQUIRED` publishes leases; `DISABLED` keeps the server local. |
| `provider.lease-seconds`                |       `30` | Provider lease TTL.                                             |
| `provider.heartbeat-interval-seconds`   |       `10` | Heartbeat interval; it must be shorter than the lease TTL.      |
| `provider.graceful-shutdown-timeout-ms` |    `10000` | Server drain timeout.                                           |
| `provider.metadata`                     |      empty | User metadata; reserved framework prefixes are rejected.        |
| `provider.metadata.yuheng.weight`      | contract `weight` or `100` | Published instance capacity, valid range `1..10000`. |

The Provider starts its gRPC server, prepares handlers as unavailable, registers
one Tianshu lease per service identity, and marks the matching handler available only
after registration succeeds. Failed or stale leases make the handler unavailable
until recovery obtains a new lease. Shutdown disables handlers, stops recovery and
heartbeats, deregisters exact leases, and drains the server.

### Consumer properties

| Property                                |            Default | Description                                              |
|-----------------------------------------|-------------------:|----------------------------------------------------------|
| `consumer.enabled`                      |            `false` | Enables Consumer proxies and discovery integration.      |
| `consumer.default-timeout-ms`           |             `3000` | Default unary deadline ceiling.                          |
| `consumer.yuheng-discovery-timeout-ms` |             `5000` | Yuheng discovery and channel-ready timeout.             |
| `consumer.yuheng-service-name`         | `egon-yuheng-rpc` | Exact Yuheng service identity.                          |
| `consumer.yuheng-group`                |          `default` | Exact Yuheng group.                                     |
| `consumer.yuheng-version`              |            `1.0.0` | Exact Yuheng version.                                   |
| `consumer.yuheng-biz-code`             |              empty | Optional Tianshu business-scope override.                    |
| `consumer.yuheng-app-code`             |              empty | Optional Tianshu application-scope override.                 |
| `consumer.channel-drain-timeout-ms`     |             `5000` | Drain timeout for replaced Provider channels.            |
| `consumer.yuheng-max-attempts`         |                `2` | Maximum Yuheng channels considered by one logical call. |
| `consumer.max-retries`                  |                `3` | Default same-mode availability retry budget.             |
| `consumer.default-load-balance`         |       `ROUND_ROBIN` | Default Consumer-side selection strategy.                |
| `consumer.consistent-hash-virtual-nodes`|              `160` | Ring density for `CONSISTENT_HASH`.                      |
| `consumer.generic-cache-max-entries`    |              `256` | Maximum generic target entries.                          |
| `consumer.generic-cache-idle-timeout-ms`|           `600000` | Idle eviction threshold for generic target entries.      |

`@EgonRpcReference` carries the common timeout, retry, load-balance, fallback, and
hash-resolver policy fields. Its `mode` is fixed when the field is injected and
defaults to `DIRECT`; an unavailable Yuheng is never replaced by a Direct
Provider, and a Direct failure never enters Yuheng. Direct mode additionally
requires `bizCode` and `appCode` (with optional `env`). Yuheng mode must leave
those direct-only fields empty. Availability-only failures (acquisition errors
and Provider/Yuheng-stage `UNAVAILABLE`) may select another candidate in the
same mode until the total deadline or retry budget is exhausted. Business
statuses such as `INVALID_ARGUMENT`, `PERMISSION_DENIED`, `NOT_FOUND`,
`FAILED_PRECONDITION`, `ALREADY_EXISTS`, `ABORTED`, and business exceptions are
terminal and are not retried. The framework does not infer idempotency; when
retries are enabled the business operation must be duplicate-safe (for example,
a unique document number with overwrite/upsert semantics).

#### Reference annotation migration

Replace the removed `@EgonRpcDirectReference` with `@EgonRpcReference` and keep
its `bizCode`, `appCode`, and optional `env` values. Existing Yuheng references
must add `mode = RpcReferenceMode.GATEWAY`; a reference without a mode is now
Direct and must provide the direct Provider identity.

### Identity and transport security

| Property                                | Description                                                                            |
|-----------------------------------------|----------------------------------------------------------------------------------------|
| `identity.env`                          | Process environment when the Tianshu adapter is not supplying the identity.                |
| `identity.host`                         | Advertised process host; otherwise the local host address is used.                     |
| `identity.instance-id`                  | Stable process instance identifier; otherwise application, host, and PID are combined. |
| `tls.enabled`                           | Enables mTLS for business RPC.                                                         |
| `tls.development-plaintext`             | Required explicit switch when `tls.enabled=false`; use only for development.           |
| `tls.certificate-chain-path`            | Client certificate chain and Provider server certificate path.                         |
| `tls.private-key-path`                  | Client private key and Provider server private key path.                               |
| `tls.trust-certificate-collection-path` | Trust collection path.                                                                 |

When `tls.enabled=true`, all three certificate paths must be readable. Provider
registration publishes whether the endpoint is secure. Tianshu transport has a
separate configuration namespace, `egon.cola.component.tianshu.rpc`, and separate
`runtime`, `registry`, and `management` HMAC credentials. See
the [Tianshu README](../../egon-cola-xingyuan/egon-cola-tianshu/README.md)
for the full Tianshu configuration contract.

### Tianshu RPC properties

The Tianshu Adapter uses a locally configured direct gRPC target:

| Property under `egon.cola.component.tianshu.rpc` |       Default | Description                                               |
|----------------------------------------------|--------------:|-----------------------------------------------------------|
| `target`                                     |          none | Required target, for example `dns:///127.0.0.1:19080`.    |
| `connect-timeout`                            |          `3s` | Tianshu channel connection timeout.                           |
| `default-timeout`                            |         `10s` | Default Tianshu call timeout.                                 |
| `load-balancing-policy`                      | `round_robin` | gRPC target load-balancing policy.                        |
| `max-inbound-message-size`                   |     `4194304` | Maximum inbound message size in bytes.                    |
| `shutdown-timeout`                           |          `5s` | Tianshu channel shutdown wait.                                |
| `tls.development-plaintext`                  |        `true` | Explicit local-development plaintext default for Tianshu RPC. |
| `auth.enabled`                               |        `true` | Enables Tianshu RPC HMAC metadata.                            |

The Tianshu registry path additionally needs `egon.cola.component.tianshu.registry.enabled=true`,
a Redisson-backed Tianshu Redis client, and the registry credential. Tianshu ConfigData
is independent: `tianshu.enabled=true` enables the configuration-client lifecycle;
registry leases can remain enabled without enabling remote configuration.

## Usage

### 1. Define the Protobuf contract

Keep the wire contract in `src/main/proto`:

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

The project parent manages `protobuf-maven-plugin`, `protoc`, and
`protoc-gen-grpc-java`. A contract module needs the standard `compile` and
`compile-custom` goals; see the test contract module for a complete plugin setup.

### 2. Bind the generated service to Java

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

The generated Proto service name, `group`, and `version` form the service
identity. The validator requires one request parameter, one Protobuf response,
an existing generated method with matching input/output descriptors, and unary
non-streaming semantics. Overloaded Java method names are rejected. Invalid
contracts fail with `RPC_INVALID_CONTRACT` during startup or proxy creation.

### 3. Expose a Provider

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

`@EgonRpcProvider` is a Spring component marker. The bean must implement at least
one `@EgonRpcService` interface. When Tianshu registry integration is active, the
Provider advertises `transport=grpc`, `serialization=protobuf`, and the filtered
Starter runtime version together with user metadata.

### 4. Call through a Yuheng

```java
@Component
public class EchoClient {

    @EgonRpcReference(
            mode = RpcReferenceMode.GATEWAY,
            timeoutMs = 2000)
    private EchoRpc echoRpc;

    public EchoResponse echo(String message) {
        return echoRpc.echo(EchoRequest.newBuilder()
                .setMessage(message)
                .build());
    }
}
```

This path requires a `RpcGatewayDirectory` implementation, normally supplied by
the Tianshu Adapter. The Consumer subscribes to the exact Yuheng service/group/version,
maintains one gRPC channel per active Yuheng, and chooses among those channels.

### 5. Explicitly call a discovered Provider

For trusted internal use cases that intentionally bypass Yuheng rules:

```java
@Component
public class InternalEchoClient {

    @EgonRpcReference(
            mode = RpcReferenceMode.DIRECT,
            bizCode = "demo",
            appCode = "echo-provider",
            env = "dev"
    )
    private EchoRpc echoRpc;
}
```

This path requires a `RpcProviderDirectory`, normally supplied by the Tianshu Adapter.
It discovers `RPC_PROVIDER` entries and manages channels to active Provider
instances; it does not apply Yuheng routing, authorization, or traffic governance.

### 6. Create an infrastructure direct client

`RpcDirectClientFactory` creates a typed proxy for one explicitly configured gRPC
target, such as a Tianshu port. The returned `RpcDirectClientHandle` owns that channel
and must be closed by the caller. It is separate from `@EgonRpcReference`, which
discovers business Providers or Gateways from Tianshu.

## Core Concepts

### Contract and descriptor identity

Protobuf descriptors are the source of truth for wire service, method, request,
response, and streaming shape. Java annotations provide the binding and logical
`group`/`version`; they do not define another serialization protocol. The
component can build a descriptor snapshot containing the dependency-aware
`FileDescriptorSet` and SHA-256 digest for Yuheng reporting and compatibility
checks.

### Provider lifecycle and leases

The lifecycle is:

```text
scan @EgonRpcProvider
  -> validate contract and build handlers
  -> start gRPC server
  -> register one lease per service identity
  -> mark handler available
  -> heartbeat and recover stale leases
  -> disable, deregister exact leases, and drain on shutdown
```

If a required registry is unavailable at initial registration, fail-fast behavior
prevents a Provider from advertising a service that is not ready. A heartbeat
failure removes availability before lease recovery is attempted.

### Consumer channel modes

| Mode            | Entry point                       | Discovery          | Channel owner       | Retry boundary                                                     |
|-----------------|-----------------------------------|--------------------|---------------------|--------------------------------------------------------------------|
| Yuheng         | `@EgonRpcReference(mode=GATEWAY)` | `INTERNAL_GATEWAY` | RPC Consumer        | Same-mode candidate reselection for acquisition/`UNAVAILABLE` only |
| Direct Provider | `@EgonRpcReference` (default)     | `RPC_PROVIDER`     | RPC Consumer        | Same-mode candidate reselection for acquisition/`UNAVAILABLE` only |
| Explicit target | `RpcDirectClientFactory`          | None               | Caller-owned handle | One transport attempt                                              |

The normal Consumer path never discovers Providers directly. Yuheng provider
selection, health probing, route rules, and Provider load balancing belong to the
Yuheng platform.

The Consumer itself maintains one immutable snapshot per exact discovery query.
The Tianshu adapter owns the event listener and periodic full reconciliation; a
Consumer call reads the local snapshot and never performs a call-time Tianshu pull.
`RpcLoadBalancers` supplies `RANDOM`, `WEIGHTED_RANDOM`, `ROUND_ROBIN`,
`SMOOTH_WEIGHTED_ROUND_ROBIN`, `CONSISTENT_HASH`, and `LEAST_IN_FLIGHT`.
Weights are read from the Provider's `yuheng.weight` metadata. Consistent hash
requires a named `RpcLoadBalanceKeyResolver` for typed calls or an explicit
bounded `affinityKey` for generic calls.

### Blocking, async, generic, and multiplexed calls

Typed methods may return a Protobuf response for blocking invocation or
`CompletionStage<ProtobufResponse>` for asynchronous invocation. Both shapes use
the same unary gRPC descriptor and metadata/interceptor chain. The generic API is
intentionally raw and bounded:

```java
RpcGenericInvocation call = RpcGenericInvocation.yuheng(
        "egon.rpc.test.v1.EchoService", "default", "1.0.0",
        "egon.rpc.test.v1.EchoService/Echo", requestBytes,
        3000, 1, LoadBalance.ROUND_ROBIN, FailStrategy.FAIL_CLOSED, null);
byte[] response = genericInvoker.invokeBlocking(call);
CompletionStage<byte[]> async = genericInvoker.invokeAsync(call);
```

The only accepted method identity is the canonical gRPC
`fully.qualified.Service/Method` form; dot aliases, arbitrary Metadata, endpoint
addresses, Tianshu credentials, Map/Object serialization, streaming, and a second
generic wire service are rejected. Generic target state is bounded by the cache
settings above. One shared `ManagedChannel` is multiplexed across concurrent unary
streams for the same endpoint key and is drained on shutdown.

### Provider Guard rate limiting

RPC does not define a second limiter or add fields to `@EgonRpcProvider`. Add the
Access Guard starter explicitly to a Provider application and put the existing
annotation on the implementation method:

```java
@EgonRpcProvider
final class OrderProvider implements OrderRpc {
    @RateLimitGuard("rpc.order.create")
    public CompletionStage<OrderResponse> create(OrderRequest request) { ... }
}
```

Configure `TOKEN_BUCKET`, `LEAKY_BUCKET`, or `SLIDING_WINDOW` under the Guard rule.
The Guard strategy/factory and Local/Redisson backend own capacity, key scope,
atomicity, and failure policy. A `RATE_LIMITED` rejection is mapped by the RPC
adapter to gRPC `UNAVAILABLE` with Provider-stage and `error-type=rate-limit`
trailers; the business method is not invoked. Other Guard decisions remain on the
normal rejection path. If the Guard starter is absent, the optional RPC adapter is
not created and no rate limit is silently assumed.

### Lifecycle states and graceful shutdown

Provider states are `NEW → STARTING → READY|DEGRADED → DRAINING → STOPPED` (or
`FAILED`). READY is published only after the gRPC server is bound and every
required lease is active. Provider heartbeat is an RPC-side fixed-delay scheduler;
Tianshu only validates/renews/expirs leases and publishes changes. Consumer startup
installs all declared Directory subscriptions and the shared channel pool before
accepting calls. Shutdown closes the admission gate, stops subscriptions and
recovery, deregisters exact leases, drains in-flight unary calls until the
configured timeout, then force-closes remaining channels. `SmartLifecycle` stop
callbacks are invoked once.

`FAIL_OPEN` is an explicit degraded-result contract: an exhausted availability
attempt returns `null` (typed or generic) and callers must handle it. It must not
be used for a required business result without a local fallback or null check.

### Deadline, metadata, and status

The effective proxy deadline is the smaller of the reference timeout and the
Consumer default. Standard gRPC deadline and cancellation semantics are carried
through the call. The built-in client interceptor sends bounded ASCII metadata
for service identity, invocation/source identity, W3C Trace Context, and request
identity. Provider code can read validated invocation data through
`RpcInvocationMetadata.current()`.

Selected status mappings are:

| gRPC status/marker                                      | `EgonRpcErrorCode`         |
|---------------------------------------------------------|----------------------------|
| `DEADLINE_EXCEEDED`                                     | `RPC_DEADLINE_EXCEEDED`    |
| `CANCELLED`                                             | `RPC_CANCELLED`            |
| `UNAVAILABLE` without Provider marker                   | `RPC_YUHENG_UNAVAILABLE`  |
| `UNAVAILABLE` with `x-egon-rpc-failure-stage: provider` | `RPC_PROVIDER_UNAVAILABLE` |
| `INVALID_ARGUMENT`                                      | `RPC_INVALID_REQUEST`      |
| `PERMISSION_DENIED`                                     | `RPC_PROVIDER_REJECTED`    |
| `UNIMPLEMENTED` or method-not-found marker              | `RPC_METHOD_NOT_FOUND`     |
| `NOT_FOUND` without method marker                       | `RPC_SERVICE_NOT_FOUND`    |

Provider exception mappers and Yuheng forwarding must preserve the failure-stage
marker when translating downstream failures.

### Yuheng and Tianshu boundaries

The Tianshu Adapter owns direct Tianshu RPC clients for three capability areas: ConfigData
runtime, service registry, and management. The Tianshu registry is the source for
temporary Provider/Yuheng leases and live snapshots. The Yuheng platform owns
the production data plane and consumes the RPC contract catalog/snapshot through
its own starter. See the [Yuheng README](../../egon-cola-xingyuan/egon-cola-yuheng/README.md)
for route, rule, health, security, and traffic-governance behavior.

## Extension Points

The Starter keeps discovery and registration behind ports so applications do not
need to depend on Tianshu implementation types:

| Extension point                  | Purpose                                                                       |
|----------------------------------|-------------------------------------------------------------------------------|
| `RpcProviderRegistry`            | Provide Provider lease registration, heartbeat, and deregistration.           |
| `RpcGatewayDirectory`            | Supply live Yuheng snapshots to Yuheng-mode Consumers.                      |
| `RpcProviderDirectory`           | Supply live Provider snapshots to `@EgonRpcReference(mode=DIRECT)` Consumers. |
| `RpcClientInterceptorFactory`    | Add an ordered request-aware gRPC client interceptor.                         |
| `RpcProviderExceptionMapper`     | Map a Provider domain exception to gRPC status and trailers.                  |
| `RpcProviderMetadataContributor` | Add registration metadata for a service identity.                             |
| `RpcInvocationChannelProvider`   | Supply a custom channel-selection/lifecycle strategy to a proxy factory.      |
| `RpcProcessIdentityProvider`     | Supply application, environment, host, and instance identity.                 |
| `RpcContractCatalog`             | Replace or adapt the validated contract catalog used by integrations.         |
| Spring `ServerInterceptor` beans | Add Provider-side gRPC server interceptors.                                   |

The production Yuheng's routing, authorization, load-balancing, circuit-breaking,
and rate-limiting extension points are outside this module and must be implemented
in the Yuheng platform.

## Project Structure

```text
egon-cola-component-rpc/
├── pom.xml
├── README.md
├── README.zh-CN.md
├── egon-cola-component-rpc-starter/
│   └── src/main/java/top/egon/cola/component/rpc/
│       ├── annotation/       # Contract, Provider, and reference annotations
│       ├── config/            # Spring Boot properties and auto-configuration
│       ├── contract/          # Descriptor, validation, catalog, and snapshots
│       ├── consumer/          # Proxy, directories, channels, and interceptors
│       ├── context/            # Process identity and invocation metadata
│       ├── exception/          # Stable RPC exception and status mapping
│       └── provider/           # Binding, server, availability, and leases
├── egon-cola-component-rpc-tianshu-adapter/
│   └── src/main/java/top/egon/cola/component/rpc/tianshu/
│       ├── autoconfigure/      # Tianshu RPC properties and Spring wiring
│       ├── client/             # Config, registry, and management clients
│       ├── contract/            # Tianshu Protobuf-facing Java contracts
│       ├── mapping/             # Protobuf/domain mappers and status mapping
│       ├── registry/            # Tianshu-backed RPC directories and registry port
│       └── security/            # HMAC canonicalization and metadata signing
└── egon-cola-component-rpc-test/
    ├── ...-test-contract/      # Echo Proto and generated contract
    ├── ...-test-provider/      # Provider process fixture
    └── ...-test-consumer/      # Consumer process fixture and Mock Yuheng tests
```

## Compatibility

- **Java:** 21+.
- **Spring Boot:** 3.5.x; the current parent manages 3.5.16.
- **gRPC/Protobuf:** gRPC Java 1.75.0, Protobuf Java/protoc 4.32.0, and
  `protoc-gen-grpc-java` 1.75.0 are the repository compatibility baseline.
- **Wire contract:** V1 accepts generated Protobuf `Message` request/response
  types and unary, non-streaming gRPC methods only.
- **Discovery contract:** Tianshu-backed discovery expects the current Tianshu registry
  service identities and lease model. A custom registry must implement the
  Starter ports rather than imitate Tianshu internals.
- **Schema evolution:** keep generated service/method names and Protobuf field
  compatibility stable; descriptor SHA-256 changes are meaningful to Yuheng
  interface reporting and compatibility checks.
- **Security:** plaintext is an explicit development mode. Production endpoints
  should use mTLS and capability-specific Tianshu HMAC credentials.

## Roadmap

The following items are not part of the current V1 runtime contract and require
separate contract/design decisions before implementation:

- Streaming RPC support and its Yuheng descriptor/reporting model.
- Streaming RPC support and its Yuheng descriptor/reporting model.
- Production-scale observability dashboards and fault-drill automation; the
  bounded runtime hooks and status/trailer contracts are already available.
- Live Tianshu/Redis topology and deployment-specific mTLS validation, which remain
  environment-owned evidence rather than module unit-test behavior.

## Validation

Run the ordinary RPC module tests from the repository root:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract \
  -am test
```

The ordinary suite uses real loopback TCP with a test Mock Yuheng and direct
Provider fixtures; it does not prove a production Tianshu/Redis/Yuheng topology.
The opt-in process test requires
an externally managed Redis instance:

```bash
TIANSHU_TEST_REDIS_HOST=127.0.0.1 \
TIANSHU_TEST_REDIS_PORT=6379 \
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract \
  -am -Pddc-live-test -Dit.test=RpcProcessIT verify
```
