# Egon COLA RPC

[English](README.md) | [中文](README.zh-CN.md)

`egon-cola-component-rpc` is the RPC transport component for Egon COLA. It
binds generated Protobuf/gRPC services to Java interfaces, provides Spring Boot
Provider and Consumer lifecycles, and integrates service leases and discovery
through the Dynamic Config Center (DDC).

The component is deliberately not a Gateway data plane. The production
Gateway, routing rules, provider health policy, traffic governance, and HTTP/RPC
forwarding are owned by the separate [Gateway platform](../../egon-cola-platforms/egon-cola-platform-gateway/README.md).

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
  availability gating, DDC lease registration, heartbeat, recovery, and exact
  lease deregistration.
- Consumer JDK proxies with two selectable runtime paths:
  `@EgonRpcReference` through discovered internal Gateways, or
  `@EgonRpcDirectReference` through discovered Providers.
- Programmatic direct gRPC clients for infrastructure endpoints through
  `RpcDirectClientFactory`; the caller owns the returned channel handle.
- Deadline and cancellation propagation, bounded trace/request metadata, and
  stable `EgonRpcErrorCode` mapping from gRPC status values.
- Optional DDC RPC adapter for ConfigData, service registry, and management
  clients, with explicit mTLS or development-plaintext configuration and
  capability-specific HMAC credentials.
- Protobuf descriptor snapshots that can be consumed by the Gateway reporting
  starter without creating a second schema or serialization model.

## Architecture

### Runtime topology

```text
Business Consumer
  ├─ @EgonRpcReference ──> active INTERNAL_GATEWAY set ──> RPC_PROVIDER ──> Provider
  └─ @EgonRpcDirectReference ──> DDC RPC registry ──> RPC_PROVIDER ──> Provider

Provider ── register / heartbeat / deregister ─┐
Gateway  ── register / heartbeat / deregister ─┼─> direct DDC gRPC target ─> DDC Admin ─> Redis
Consumer ── discover / subscribe Gateway or Provider ────────────────────────┘
```

The DDC target is local bootstrap configuration. DDC is not discovered through
itself. Provider and Gateway registrations are temporary Redis leases; each
registration has an `instanceId + leaseId` identity, and heartbeat/deregistration
must match the complete lease identity.

### Responsibility boundary

| Layer               | Owned by this component                                                        | Not owned by this component                                   |
|---------------------|--------------------------------------------------------------------------------|---------------------------------------------------------------|
| Contract            | Java binding, descriptor validation, unary contract snapshot                   | A second IDL or serializer                                    |
| Provider            | gRPC server, bean dispatch, availability, lease lifecycle                      | Gateway routing or provider health probing                    |
| Consumer            | Proxy, channel lifecycle, deadline, metadata, Gateway/Provider directory ports | Gateway rules, provider load balancing, business retry policy |
| DDC adapter         | Direct gRPC clients for config, registry, and management ports                 | DDC Admin persistence and Redis implementation                |
| Gateway integration | Transport-neutral Gateway/Provider directory interfaces and contract catalog   | Production Gateway data plane and control plane               |

For the normal business path, the Consumer discovers only `INTERNAL_GATEWAY`.
It does not query `RPC_PROVIDER` or open Provider channels. The direct Provider
path is an explicit alternative for trusted/internal calls and is selected only
by `@EgonRpcDirectReference`.

## Requirements

- JDK 21 or later. The components parent enforces Java 21.
- Maven Wrapper from the repository (`./mvnw`) or a compatible Maven installation.
- Spring Boot 3.5.x when using the auto-configuration.
- Generated Java and gRPC classes produced by `protoc` and
  `protoc-gen-grpc-java` compatible with the versions managed by this repository:
  Protobuf 4.32.0 and gRPC Java 1.75.0.
- A reachable DDC direct RPC endpoint and Redis when Provider leases, Gateway or
  Provider discovery, or DDC ConfigData is enabled.
- Matching least-privilege DDC HMAC credentials for each enabled capability;
  local development may explicitly use plaintext, but production deployments
  should configure mTLS.

## Quick Start

The shortest Spring Boot setup uses the DDC adapter, one Protobuf contract, one
Provider application, and one Gateway-backed Consumer application.

1. Put the `.proto` file in `src/main/proto` and generate Java/gRPC sources.
2. Declare a Java interface with `@EgonRpcService` and
   `@EgonRpcMethod`.
3. Implement the interface on an `@EgonRpcProvider` Spring bean.
4. Inject the interface with `@EgonRpcReference` in the Consumer.
5. Enable RPC, the relevant role, and DDC registry access. For a local
   plaintext setup, set both DDC and business RPC plaintext switches explicitly.

Minimal local development shape:

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

For a Consumer, use the same DDC registry scope and replace the role block with:

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

The DDC Admin, Redis, production Gateway, and Gateway rules are outside this
component's Quick Start. Use
the [Gateway and DDC integration runbook](../../egon-cola-platforms/egon-cola-platform-gateway/docs/developer-integration.md)
for a complete multi-process topology.

## Maven Dependency

Import the Components BOM so the Starter and optional DDC Adapter use the
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

Use the DDC Adapter for DDC ConfigData, registry-backed Provider/Gateway
discovery, or DDC management RPC. It brings the Starter and DDC SDK transitively:

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-rpc-ddc-adapter</artifactId>
</dependency>
```

The `egon-cola-component-rpc-test` aggregator and its contract/provider/consumer
applications are test-only modules and are not exported by the BOM.

## Configuration

### RPC auto-configuration

The Starter imports `EgonRpcAutoConfig` only when
`egon.cola.component.rpc.enabled=true`. Provider and Consumer beans are created
independently through their respective `provider.enabled` and `consumer.enabled`
flags. The DDC Adapter imports `DdcRpcAutoConfiguration` and exposes DDC clients
only when the corresponding DDC features are enabled.

### Provider properties

All properties in this table are under `egon.cola.component.rpc`.

| Property                                |    Default | Description                                                     |
|-----------------------------------------|-----------:|-----------------------------------------------------------------|
| `enabled`                               |    `false` | Enables RPC auto-configuration.                                 |
| `provider.enabled`                      |    `false` | Enables Provider scanning, server, and lifecycle.               |
| `provider.bind-address`                 |  `0.0.0.0` | gRPC server bind address.                                       |
| `provider.port`                         |    `19090` | gRPC server port; `0` is useful for tests.                      |
| `provider.advertised-host`              | local host | Routable host published to DDC.                                 |
| `provider.advertised-port`              | bound port | Routable port published to DDC.                                 |
| `provider.registration-fail-fast`       |     `true` | Fails startup when required initial registration fails.         |
| `provider.registration-mode`            | `REQUIRED` | `REQUIRED` publishes leases; `DISABLED` keeps the server local. |
| `provider.lease-seconds`                |       `30` | Provider lease TTL.                                             |
| `provider.heartbeat-interval-seconds`   |       `10` | Heartbeat interval; it must be shorter than the lease TTL.      |
| `provider.graceful-shutdown-timeout-ms` |    `10000` | Server drain timeout.                                           |
| `provider.metadata`                     |      empty | User metadata; reserved framework prefixes are rejected.        |

The Provider starts its gRPC server, prepares handlers as unavailable, registers
one DDC lease per service identity, and marks the matching handler available only
after registration succeeds. Failed or stale leases make the handler unavailable
until recovery obtains a new lease. Shutdown disables handlers, stops recovery and
heartbeats, deregisters exact leases, and drains the server.

### Consumer properties

| Property                                |            Default | Description                                              |
|-----------------------------------------|-------------------:|----------------------------------------------------------|
| `consumer.enabled`                      |            `false` | Enables Consumer proxies and discovery integration.      |
| `consumer.default-timeout-ms`           |             `3000` | Default unary deadline ceiling.                          |
| `consumer.gateway-discovery-timeout-ms` |             `5000` | Gateway discovery and channel-ready timeout.             |
| `consumer.gateway-service-name`         | `egon-gateway-rpc` | Exact Gateway service identity.                          |
| `consumer.gateway-group`                |          `default` | Exact Gateway group.                                     |
| `consumer.gateway-version`              |            `1.0.0` | Exact Gateway version.                                   |
| `consumer.gateway-biz-code`             |              empty | Optional DDC business-scope override.                    |
| `consumer.gateway-app-code`             |              empty | Optional DDC application-scope override.                 |
| `consumer.channel-drain-timeout-ms`     |             `5000` | Drain timeout for replaced Provider channels.            |
| `consumer.gateway-max-attempts`         |                `2` | Maximum Gateway channels considered by one logical call. |

`@EgonRpcReference.timeoutMs` may shorten but never extend the configured
`default-timeout-ms`. Gateway failover is limited to an idempotent method that
receives a Gateway-stage `UNAVAILABLE`; Provider failures and direct Provider
calls are not retried by the Consumer. Channels explicitly disable gRPC's own
transport retry policy.

### Identity and transport security

| Property                                | Description                                                                            |
|-----------------------------------------|----------------------------------------------------------------------------------------|
| `identity.env`                          | Process environment when the DDC adapter is not supplying the identity.                |
| `identity.host`                         | Advertised process host; otherwise the local host address is used.                     |
| `identity.instance-id`                  | Stable process instance identifier; otherwise application, host, and PID are combined. |
| `tls.enabled`                           | Enables mTLS for business RPC.                                                         |
| `tls.development-plaintext`             | Required explicit switch when `tls.enabled=false`; use only for development.           |
| `tls.certificate-chain-path`            | Client certificate chain and Provider server certificate path.                         |
| `tls.private-key-path`                  | Client private key and Provider server private key path.                               |
| `tls.trust-certificate-collection-path` | Trust collection path.                                                                 |

When `tls.enabled=true`, all three certificate paths must be readable. Provider
registration publishes whether the endpoint is secure. DDC transport has a
separate configuration namespace, `egon.cola.component.ddc.rpc`, and separate
`runtime`, `registry`, and `management` HMAC credentials. See
the [DDC README](../../egon-cola-platforms/egon-cola-platform-dynamic-config-center/README.md)
for the full DDC configuration contract.

### DDC RPC properties

The DDC Adapter uses a locally configured direct gRPC target:

| Property under `egon.cola.component.ddc.rpc` |       Default | Description                                               |
|----------------------------------------------|--------------:|-----------------------------------------------------------|
| `target`                                     |          none | Required target, for example `dns:///127.0.0.1:19080`.    |
| `connect-timeout`                            |          `3s` | DDC channel connection timeout.                           |
| `default-timeout`                            |         `10s` | Default DDC call timeout.                                 |
| `load-balancing-policy`                      | `round_robin` | gRPC target load-balancing policy.                        |
| `max-inbound-message-size`                   |     `4194304` | Maximum inbound message size in bytes.                    |
| `shutdown-timeout`                           |          `5s` | DDC channel shutdown wait.                                |
| `tls.development-plaintext`                  |        `true` | Explicit local-development plaintext default for DDC RPC. |
| `auth.enabled`                               |        `true` | Enables DDC RPC HMAC metadata.                            |

The DDC registry path additionally needs `egon.cola.component.ddc.registry.enabled=true`,
a Redisson-backed DDC Redis client, and the registry credential. DDC ConfigData
is independent: `ddc.enabled=true` enables the configuration-client lifecycle;
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

    @EgonRpcMethod(name = "Echo", idempotent = true)
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
one `@EgonRpcService` interface. When DDC registry integration is active, the
Provider advertises `transport=grpc`, `serialization=protobuf`, and the filtered
Starter runtime version together with user metadata.

### 4. Call through a Gateway

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

This path requires a `RpcGatewayDirectory` implementation, normally supplied by
the DDC Adapter. The Consumer subscribes to the exact Gateway service/group/version,
maintains one gRPC channel per active Gateway, and chooses among those channels.

### 5. Explicitly call a discovered Provider

For trusted internal use cases that intentionally bypass Gateway rules:

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

This path requires a `RpcProviderDirectory`, normally supplied by the DDC Adapter.
It discovers `RPC_PROVIDER` entries and manages channels to active Provider
instances; it does not apply Gateway routing, authorization, traffic governance,
or Provider retry policy.

### 6. Create an infrastructure direct client

`RpcDirectClientFactory` creates a typed proxy for one explicitly configured gRPC
target, such as a DDC port. The returned `RpcDirectClientHandle` owns that channel
and must be closed by the caller. It is separate from
`@EgonRpcDirectReference`, which discovers business Providers from DDC.

## Core Concepts

### Contract and descriptor identity

Protobuf descriptors are the source of truth for wire service, method, request,
response, and streaming shape. Java annotations provide the binding and logical
`group`/`version`; they do not define another serialization protocol. The
component can build a descriptor snapshot containing the dependency-aware
`FileDescriptorSet` and SHA-256 digest for Gateway reporting and compatibility
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

| Mode            | Entry point               | Discovery          | Channel owner       | Retry boundary                                                        |
|-----------------|---------------------------|--------------------|---------------------|-----------------------------------------------------------------------|
| Gateway         | `@EgonRpcReference`       | `INTERNAL_GATEWAY` | RPC Consumer        | Idempotent Gateway-stage `UNAVAILABLE` only, within the same deadline |
| Direct Provider | `@EgonRpcDirectReference` | `RPC_PROVIDER`     | RPC Consumer        | One Provider attempt; no business retry                               |
| Explicit target | `RpcDirectClientFactory`  | None               | Caller-owned handle | One transport attempt                                                 |

The normal Consumer path never discovers Providers directly. Gateway provider
selection, health probing, route rules, and Provider load balancing belong to the
Gateway platform.

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
| `UNAVAILABLE` without Provider marker                   | `RPC_GATEWAY_UNAVAILABLE`  |
| `UNAVAILABLE` with `x-egon-rpc-failure-stage: provider` | `RPC_PROVIDER_UNAVAILABLE` |
| `INVALID_ARGUMENT`                                      | `RPC_INVALID_REQUEST`      |
| `PERMISSION_DENIED`                                     | `RPC_PROVIDER_REJECTED`    |
| `UNIMPLEMENTED` or method-not-found marker              | `RPC_METHOD_NOT_FOUND`     |
| `NOT_FOUND` without method marker                       | `RPC_SERVICE_NOT_FOUND`    |

Provider exception mappers and Gateway forwarding must preserve the failure-stage
marker when translating downstream failures.

### Gateway and DDC boundaries

The DDC Adapter owns direct DDC RPC clients for three capability areas: ConfigData
runtime, service registry, and management. The DDC registry is the source for
temporary Provider/Gateway leases and live snapshots. The Gateway platform owns
the production data plane and consumes the RPC contract catalog/snapshot through
its own starter. See the [Gateway README](../../egon-cola-platforms/egon-cola-platform-gateway/README.md)
for route, rule, health, security, and traffic-governance behavior.

## Extension Points

The Starter keeps discovery and registration behind ports so applications do not
need to depend on DDC implementation types:

| Extension point                  | Purpose                                                                  |
|----------------------------------|--------------------------------------------------------------------------|
| `RpcProviderRegistry`            | Provide Provider lease registration, heartbeat, and deregistration.      |
| `RpcGatewayDirectory`            | Supply live Gateway snapshots to Gateway-mode Consumers.                 |
| `RpcProviderDirectory`           | Supply live Provider snapshots to direct-reference Consumers.            |
| `RpcClientInterceptorFactory`    | Add an ordered request-aware gRPC client interceptor.                    |
| `RpcProviderExceptionMapper`     | Map a Provider domain exception to gRPC status and trailers.             |
| `RpcProviderMetadataContributor` | Add registration metadata for a service identity.                        |
| `RpcInvocationChannelProvider`   | Supply a custom channel-selection/lifecycle strategy to a proxy factory. |
| `RpcProcessIdentityProvider`     | Supply application, environment, host, and instance identity.            |
| `RpcContractCatalog`             | Replace or adapt the validated contract catalog used by integrations.    |
| Spring `ServerInterceptor` beans | Add Provider-side gRPC server interceptors.                              |

The production Gateway's routing, authorization, load-balancing, circuit-breaking,
and rate-limiting extension points are outside this module and must be implemented
in the Gateway platform.

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
├── egon-cola-component-rpc-ddc-adapter/
│   └── src/main/java/top/egon/cola/component/rpc/ddc/
│       ├── autoconfigure/      # DDC RPC properties and Spring wiring
│       ├── client/             # Config, registry, and management clients
│       ├── contract/            # DDC Protobuf-facing Java contracts
│       ├── mapping/             # Protobuf/domain mappers and status mapping
│       ├── registry/            # DDC-backed RPC directories and registry port
│       └── security/            # HMAC canonicalization and metadata signing
└── egon-cola-component-rpc-test/
    ├── ...-test-contract/      # Echo Proto and generated contract
    ├── ...-test-provider/      # Provider process fixture
    └── ...-test-consumer/      # Consumer process fixture and Mock Gateway tests
```

## Compatibility

- **Java:** 21+.
- **Spring Boot:** 3.5.x; the current parent manages 3.5.16.
- **gRPC/Protobuf:** gRPC Java 1.75.0, Protobuf Java/protoc 4.32.0, and
  `protoc-gen-grpc-java` 1.75.0 are the repository compatibility baseline.
- **Wire contract:** V1 accepts generated Protobuf `Message` request/response
  types and unary, non-streaming gRPC methods only.
- **Discovery contract:** DDC-backed discovery expects the current DDC registry
  service identities and lease model. A custom registry must implement the
  Starter ports rather than imitate DDC internals.
- **Schema evolution:** keep generated service/method names and Protobuf field
  compatibility stable; descriptor SHA-256 changes are meaningful to Gateway
  interface reporting and compatibility checks.
- **Security:** plaintext is an explicit development mode. Production endpoints
  should use mTLS and capability-specific DDC HMAC credentials.

## Roadmap

The following items are not part of the current V1 runtime contract and require
separate contract/design decisions before implementation:

- Streaming RPC support and its Gateway descriptor/reporting model.
- A single effective retry/failure-policy model connecting contract metadata,
  Consumer references, and Gateway policies.
- Consistent runtime handling for declaration metadata such as reference-level
  `group`, `version`, `loadBalance`, `retries`, and `failStrategy`.
- Richer metrics and channel/provider diagnostics without exposing unbounded
  request metadata or sensitive DDC credentials.
- Expanded production integration documentation and fault-drill coverage for
  mTLS, lease recovery, Gateway failover, and descriptor compatibility.

## Validation

Run the ordinary RPC module tests from the repository root:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract \
  -am test
```

The ordinary suite uses real loopback TCP with a test Mock Gateway and does not
prove a production DDC/Redis/Gateway topology. The opt-in process test requires
an externally managed Redis instance:

```bash
DDC_TEST_REDIS_HOST=127.0.0.1 \
DDC_TEST_REDIS_PORT=6379 \
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract \
  -am -Pddc-live-test -Dit.test=RpcProcessIT verify
```
