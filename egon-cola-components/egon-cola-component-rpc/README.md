# Egon COLA RPC

[English](README.md) | [中文](README.zh-CN.md)

## Scope

`egon-cola-component-rpc` is a small Spring Boot RPC framework built on standard
gRPC Java and Protobuf. A Provider exposes annotated Java beans and registers
their generated Proto service identities in the Dynamic Config Center (DDC). A
Consumer creates JDK proxies, discovers exactly one internal Gateway, and sends
every call to that Gateway.

The production Gateway is deliberately outside this component. The repository
contains a Mock Gateway under test sources only, so the complete
Consumer → Gateway → Provider path can be verified before the production
Gateway is developed.

V1 supports unary methods whose request and response implement Protobuf
`Message`. The `.proto` file is the only IDL. Java annotations bind generated
descriptors to Java interfaces; they do not define another service, message, or
serialization protocol.

## Modules

| Module | Responsibility | Published |
|---|---|---|
| `egon-cola-component-rpc-starter` | Provider server, Consumer proxy, DDC registration/discovery, metadata, deadlines, and exception mapping | Yes, and listed in the Components BOM |
| `egon-cola-component-rpc-test` | Test-only aggregator | No |
| `...-test-contract` | One Echo Proto and its generated Java/gRPC classes | No |
| `...-test-provider` | Provider test application | No |
| `...-test-consumer` | Consumer test application | No |
| `...-test-suite` | Mock Gateway, real-TCP tests, and opt-in process verification | No |

The RPC root intentionally contains only the Starter and Test aggregators. Test
artifacts are never added to the public BOM.

## Runtime Topology

```text
Consumer ── one gRPC channel ──> unique active internal Gateway
                                      │
                                      ├── discovers RPC_PROVIDER service groups
                                      ├── selects an instance
                                      └── forwards the unary call ──> Provider

Provider ── register/heartbeat/deregister ─┐
Gateway  ── register/heartbeat/deregister ─┼──> one DDC Admin ──> one Redis
Consumer ── discover/subscribe Gateway ────┘
```

DDC V1 is standalone: one Admin and one Redis single-server connection. Provider
and Gateway registrations are temporary Redis leases. Every registration gets
a new `leaseId`; heartbeat and deregistration atomically match
`instanceId + leaseId`.

The Consumer never queries `RPC_PROVIDER`, never opens a Provider channel, and
contains no load-balancing algorithm. Zero active Gateways at the startup
deadline, more than one active Gateway, or loss of the only active Gateway
causes a fast failure.

## Dependency

Import the Components BOM and add only the Starter to production applications:

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

## Proto Code Generation

Keep the contract in `src/main/proto` and use standard `protoc` plus
`protoc-gen-grpc-java`. The project parent already manages the shown plugin and
tool versions:

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

Example IDL:

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

Bind the generated gRPC service to a Java interface:

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

At Provider and Consumer startup, strict validation requires:

- an annotated interface with at least one method;
- no Java method overloads;
- exactly one request parameter and one response value;
- generated Protobuf `Message` request and response types;
- an existing generated gRPC method with the same input and output descriptors;
- unary, non-streaming semantics.

Any mismatch fails startup with `RPC_INVALID_CONTRACT`.

## Provider

Implement the contract on a Spring bean:

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

Configure DDC registry access and the Provider:

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

Provider properties:

| Property under `egon.cola.component.rpc` | Default | Meaning |
|---|---:|---|
| `enabled` | `false` | Enables RPC auto-configuration |
| `provider.enabled` | `false` | Enables Provider scanning, server, and leases |
| `provider.bind-address` | `0.0.0.0` | gRPC server bind address |
| `provider.port` | `19090` | gRPC server port; `0` selects a free test port |
| `provider.advertised-host` | process host | Routable host registered in DDC |
| `provider.advertised-port` | bound port | Routable port registered in DDC |
| `provider.registration-fail-fast` | `true` | Fails startup when initial registration fails |
| `provider.lease-seconds` | `30` | Provider lease TTL |
| `provider.heartbeat-interval-seconds` | `10` | Provider heartbeat interval |
| `provider.graceful-shutdown-timeout-ms` | `10000` | Server drain timeout |
| `provider.metadata` | empty | User metadata without reserved prefixes |

`ddc.enabled: false` disables the configuration-client lifecycle only; registry
access remains enabled independently. Applications that also consume dynamic
configuration may set it to `true`.

The Provider starts its gRPC server, registers one DDC lease per exposed service,
then marks handlers available. A failed or mismatched heartbeat makes the
handler unavailable until a new lease is acquired. Shutdown first disables
handlers and lease recovery, then stops heartbeats, actively deregisters exact
leases, and drains the server before forcing termination.

Provider metadata may be configured under `provider.metadata`, but user entries
must not use `ddc.`, `egon.internal.`, or `egon.rpc.` prefixes. The framework
itself registers its transport, serialization, and runtime-version metadata.
The runtime version is loaded from the filtered Starter resource; an explicit
`egon.rpc.runtime-version` property is only an override.

## Consumer

Inject the annotated contract into a Spring bean:

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

Configure the Consumer and the exact Gateway service identity:

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

Consumer properties:

| Property under `egon.cola.component.rpc` | Default | Meaning |
|---|---:|---|
| `enabled` | `false` | Enables RPC auto-configuration |
| `consumer.enabled` | `false` | Enables Gateway discovery and Consumer proxies |
| `consumer.default-timeout-ms` | `3000` | Maximum default unary deadline |
| `consumer.gateway-discovery-timeout-ms` | `5000` | Startup discovery and Channel-ready timeout |
| `consumer.gateway-service-name` | `egon-internal-rpc-gateway` | Exact Gateway service name |
| `consumer.gateway-group` | `default` | Exact Gateway group |
| `consumer.gateway-version` | `1.0.0` | Exact Gateway version |
| `consumer.channel-drain-timeout-ms` | `5000` | Replaced Channel drain timeout |

`@EgonRpcReference.timeoutMs` may shorten but never extend
`default-timeout-ms`. Every production Consumer channel explicitly calls
gRPC `disableRetry()`.

## Metadata, Status, and Cancellation

The Consumer sends bounded ASCII metadata for service, group, version,
invocation ID, source application, source instance, and trace ID. The Gateway
forwards it and the Provider exposes validated values through
`RpcInvocationMetadata.current()`. `traceparent` and `tracestate` are also
reserved for Gateway forwarding and Provider input; the V1 Starter
automatically creates or inherits the trace ID. Invalid or oversized values are
discarded rather than passed to business code.

gRPC statuses are converted to stable framework errors:

| gRPC status | RPC error |
|---|---|
| `DEADLINE_EXCEEDED` | `RPC_DEADLINE_EXCEEDED` |
| `CANCELLED` | `RPC_CANCELLED` |
| `UNAVAILABLE` without a downstream marker | `RPC_GATEWAY_UNAVAILABLE` |
| `UNAVAILABLE` with `x-egon-rpc-failure-stage: provider` | `RPC_PROVIDER_UNAVAILABLE` |
| `INVALID_ARGUMENT` | `RPC_INVALID_REQUEST` |
| `PERMISSION_DENIED` | `RPC_PROVIDER_REJECTED` |
| `NOT_FOUND` | `RPC_SERVICE_NOT_FOUND` or `RPC_METHOD_NOT_FOUND` |
| other status | `RPC_INTERNAL` |

Deadline and cancellation use standard gRPC propagation. V1 adds no framework
retry and does not retry business calls. A Gateway forwarding a downstream
Provider `UNAVAILABLE` status must add the framework failure-stage trailer.

## Gateway Boundary

There is no production Gateway package in the Starter. The following
capabilities exist only under `rpc-test-suite/src/test`:

- Gateway registration and heartbeat;
- Provider service-catalog and instance Directory;
- Provider Channel cache;
- dynamic byte-level Handler Registry;
- deterministic round-robin test selector;
- unary forwarder.

They are reference fixtures, not reusable production APIs. The production
Gateway will be developed after the DDC and RPC contracts are accepted.

## Tests

Ordinary tests use real loopback TCP for Consumer → Mock Gateway → Provider.
They do not use gRPC in-process transports:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite \
  -am test
```

The suite also verifies cancellation and multi-Provider Directory
discovery/replacement/eviction. The instance-selection policy belongs to the
Mock Gateway and is not a Starter capability.

An opt-in `verify` profile launches one Admin, one Provider, one Mock Gateway,
and one Consumer in separate JVMs. It requires an externally managed Redis
single server:

```bash
DDC_TEST_REDIS_HOST=127.0.0.1 \
DDC_TEST_REDIS_PORT=6379 \
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite \
  -am -Pddc-live-test -Dit.test=RpcProcessIT verify
```

Set `DDC_TEST_REDIS_PASSWORD` when Redis requires authentication. The process
test uses a temporary SQLite database, verifies Flyway V1/V2, writes child logs
under `target/rpc-process-it`, and stops children in reverse order.

## V1 Non-goals

V1 does not implement:

- Consumer direct connection to a Provider;
- Consumer-side load balancing or Provider discovery;
- gray/canary routing;
- rate limiting or circuit breaking;
- business retries;
- streaming RPC;
- non-Protobuf serialization;
- a production Gateway, Provider Directory, Gateway Handler Registry, Provider
  Channel Factory, instance selector, or Unary Forwarder.

These boundaries keep the Starter responsible for contracts, exposure,
proxies, registration/discovery integration, metadata, timeout, cancellation,
and exception behavior while traffic governance remains a Gateway concern.
