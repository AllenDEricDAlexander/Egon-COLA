# Egon COLA Dynamic Config Center

[English](README.md) | [中文](README.zh-CN.md)

## Scope

`egon-cola-platform-dynamic-config-center` provides a Spring Boot ConfigData SDK
for one YAML business-configuration document, typed management APIs, a standalone
Admin application, and a Redis-backed service registry for RPC Providers and
internal Gateways.

The Maven modules use the `egon-cola-platform-*` prefix. The Starter Java API is
organized by domain and does not retain forwarding types for its former technical
packages. The external `egon.cola.component.ddc` configuration namespace remains
unchanged.

V1 supports one logical control plane backed by shared PostgreSQL and Redis. Multiple
Admin processes can serve the same control plane: publish preparation uses PostgreSQL
row locks, conditional version updates, and persisted publish tasks, while completion
waiters fall back to polling the shared task state. It does not implement Raft, leader
election, consensus logs, or a distributed lock service. The Admin and SDK Redis clients
support `SINGLE`, `SENTINEL`, and `CLUSTER` topologies through Redisson. PostgreSQL is
the production database; SQLite is retained for tests. Service registry entries are
temporary lease state in Redis and never create JPA or database tables.

## Deployment Topology

```text
Configuration Clients ──direct gRPC/HMAC──┐
RPC Providers ────────direct gRPC/HMAC──┼──> one logical DDC target ──> Admin set ──> PostgreSQL
Internal Gateway ─────direct gRPC/HMAC──┘                                      │
                                                                                 └──> shared Redis
Configuration Clients <──── Redis Pub/Sub ────────┘
Registry Subscribers  <──── Redis Pub/Sub ────────┘
```

The Admin processes are the only machine control-plane RPC providers. Clients use
a locally configured `dns:///`, VIP, or load-balancer target and never bootstrap
DDC through DDC service discovery. Admin HTTP remains for human management APIs and
Actuator health only. The Admin processes share
PostgreSQL and Redis; no Admin-local data is authoritative for a completed publish.
Redis contains
configuration cache, publish notifications, live configuration-client leases,
and service-registry leases. PostgreSQL stores configuration, version, publish,
ACK, operation, and configuration-client projection data.

## Modules

| Module | Responsibility |
|---|---|
| `egon-cola-platform-dynamic-config-center-starter` | Transport-neutral SDK runtime and ports: ConfigData, `@DdcValue`, selective refresh, ACK, leases, management and registry contracts |
| `egon-cola-platform-dynamic-config-center-http-registration-starter` | Spring HTTP service registration, DDC lease heartbeat/recovery, and registration metadata contributor SPI |
| `egon-cola-component-rpc-ddc-adapter` | Composition adapter under `components/rpc`: protobuf contracts, direct gRPC clients/providers, HMAC metadata, and Spring Boot wiring |
| `egon-cola-platform-dynamic-config-center-admin` | Human REST Admin plus direct gRPC facades, PostgreSQL persistence, Redis cache/leases, and synchronous publish state machine |
| `egon-cola-platform-dynamic-config-center-admin-web` | Standalone management console (React + antd + Vite, pure Node project outside the Maven reactor); build and deployment instructions live in `egon-cola-platform-dynamic-config-center-admin-web/README.md` |
| `egon-cola-platform-dynamic-config-center-test` | Starter samples, black-box consumer verification, and cross-boundary admission lifecycle acceptance tests |

The Admin web UI has been extracted from the jar (`/ddc-admin` is no longer served
by Admin). The console deploys as its own container, points at Admin via
`DDC_ADMIN_API_BASE_URL`, and proxies `/api` same-origin through its static
server, so Admin needs no CORS configuration.

## Admin Page Queries

DDC Admin exposes additive server-side page queries for human management:

```text
GET /api/v1/ddc/bizs/page
GET /api/v1/ddc/namespaces/page
GET /api/v1/ddc/envs/page
GET /api/v1/ddc/apps/page
GET /api/v1/ddc/namespace-env-app-bindings/page
GET /api/v1/ddc/configs/page
GET /api/v1/ddc/configs/{id}/versions/page
GET /api/v1/ddc/publish-tasks/page
GET /api/v1/ddc/instances/page
GET /api/v1/ddc/cache/check/page
GET /api/v1/ddc/registry/services/page
GET /api/v1/ddc/registry/instances/page
```

For example:

```text
GET /api/v1/ddc/bizs/page?pageNo=1&pageSize=10
success -> PageResultRecord { records, page }
failure -> existing ResultRecord error envelope
legacy list/catalog/snapshot endpoints remain available
```

Page numbers start at 1. These REST endpoints are for Admin Web and other human
management clients. Starter and RPC machine clients continue to consume complete
catalogs and snapshots rather than the Admin page contract.

## Starter Package Layout

```text
top.egon.cola.component.ddc
├── annotation
├── autoconfigure
├── configuration
│   ├── binding
│   ├── bootstrap
│   ├── client
│   ├── environment
│   ├── format
│   ├── model
│   ├── refresh
│   ├── runtime
│   └── subscription
├── error
├── lease
├── management
│   ├── client
│   └── model
├── observability
├── registry
│   ├── client
│   ├── model
│   ├── state
│   └── subscription
└── transport
    └── redis
```

`DdcConfigClient`, `DdcServiceRegistryClient`, and `DdcManagementClient` remain
separate domain facades. The RPC-DDC Adapter implements them over three unary gRPC
services. The base starter does not depend on RPC. The HTTP registration starter is
an application composition starter and therefore includes the RPC-DDC Adapter used
to send registry operations to DDC. HTTP registration properties use the
`egon.cola.component.ddc.registry.http` namespace.

## Operations Endpoints

DDC Admin exposes `GET /actuator/health/readiness` for startup and readiness
checks. `GET /actuator/info` exposes the application name and the Maven-filtered
component version under `app.name` and `app.version`.

Executable applications add the RPC-DDC Adapter and import `ddc:application.yml` through
`spring.config.import`. `egon.cola.component.ddc.enabled=true` loads the remote YAML
during ConfigData processing, then starts the `CONFIG_CLIENT` registration, pull,
Redis subscription, heartbeat, and shutdown-offline lifecycle.
`egon.cola.component.ddc.registry.enabled=true` independently enables RPC/Gateway
service registration; those `RPC_PROVIDER`,
`HTTP_PROVIDER`, and `INTERNAL_GATEWAY` leases are not configuration-client registrations. Every enabled
remote path must locally configure the direct RPC target, matching least-privilege
HMAC credentials, and Redis topology. With `redis.enabled=false`, no registration, pull, subscription,
heartbeat, or ACK runs. Production multi-Admin access must use an external DNS name,
VIP, or HTTP/2-capable load balancer with `round_robin`; DDC never discovers its own
Admin processes and no DDC machine HTTP compatibility endpoint exists.

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-rpc-ddc-adapter</artifactId>
    <version>5.3.3</version>
</dependency>
```

## Trace Propagation

The Starter and RPC-DDC Adapter use `egon-cola-component-common-trace` in gRPC
facade calls, heartbeat, pull, ACK retry, Redis topic callbacks, and lease recovery
tasks. DDC calls triggered by a business request inherit the
current `traceId` and create a child span. Background tasks without an upstream
trace open a fresh `TraceContext` for each logical operation and restore the
worker thread MDC afterwards. Outbound requests write only `traceparent`,
`tracestate`, and `x-egon-request-id`; they do not write `x-egon-trace-id`.

The Spring MVC Admin application depends on
`egon-cola-component-common-trace-spring-boot-starter` directly and no longer keeps a
DDC-specific trace filter.

## Configuration Client Lifecycle

The initial remote YAML is loaded by Spring Boot ConfigData before beans are bound.
`DdcRuntimeCoordinator` then starts only after the Redis subscription is active and
executes this order:

1. register the configuration client and receive a new `leaseId`;
2. pull the YAML resource imported through ConfigData and reconcile it with the startup snapshot;
3. enter `READY`;
4. heartbeat on the configured interval;
5. actively take the current lease offline during shutdown.

Each registration replaces the old lease. Heartbeat and offline operations
atomically match `instanceId + leaseId`; a stale lease cannot renew or delete
the replacement lease. When a current lease is missing or mismatched, the SDK
registers again and repeats initial synchronization.

The remote `application.yml` may use arbitrary nesting, for example:

```yaml
order:
  rate-limit:
    permits-per-second: 200
  downgrade:
    enabled: false
```

`@DdcValue` uses the same expression semantics as Spring `@Value`. Dot paths address
nested properties, the value after `:` is used only when the property is absent, and
Spring's conversion service infers the target type from the field:

```java
@DdcValue("${order.rate-limit.permits-per-second:100}")
private volatile Integer permitsPerSecond;

@DdcValue("${order.downgrade.enabled:false}")
private volatile Boolean downgradeEnabled;
```

Runtime publication atomically replaces the DDC PropertySource and computes YAML
leaf changes. Explicit `DdcConfigApplier` registrations and refreshable `@DdcValue`
fields receive matching leaves. A setter-based `@ConfigurationProperties` class is
rebound only when it is annotated with `@DdcRefreshable`; immutable properties and
all other changed keys are reported as restart-required without restarting the
ApplicationContext.

## Lease Protocol

All roles use the same register, heartbeat, and deregister semantics, while
their callers use role-specific timing:

| Role | Default lease | Default heartbeat | Storage |
|---|---:|---:|---|
| `CONFIG_CLIENT` | 30 seconds | 10 seconds | Redis lease plus `ddc_instance` management projection |
| `RPC_PROVIDER` | 30 seconds | 10 seconds | Redis only |
| `INTERNAL_GATEWAY` | 15 seconds | 5 seconds | Redis only |

The Admin accepts leases from 5 to 300 seconds, and the heartbeat interval must
be shorter than the lease. Every register request creates a new `leaseId`.
Redis bucket TTL is authoritative; heartbeat never recreates a missing lease.

### IdP admission boundary

Every configuration-client, HTTP Provider, RPC Provider, and internal-Gateway
register or heartbeat request carries a fresh IdP Admission Ticket bound to the
exact `bizCode + appCode + env + instanceId`. DDC verifies the ticket signature,
dedicated token type, `ddc-registry` audience, logical Resource identity,
resource version, physical triple, instance ID, credential ID, and expiry. Its
lease expiry is capped at the Ticket expiry even when the requested lease is
longer; raw tickets are never stored in registry or audit state.

An enabled application is approved once at the Resource triple, so all of its
instances may register without per-instance approval. A process must obtain a
valid Ticket before advertising readiness. If IdP is unavailable, an existing
lease may run only until its current Ticket expires: no new registration and no
heartbeat can extend the lease beyond that boundary. The runtime enters recovery
and becomes not ready when it cannot renew safely.

Disabling a Resource publishes an IdP lifecycle event that removes only leases
whose `bizCode + appCode + env` match the disabled Resource. Re-enable the
Resource, restore a valid owner public key/credential, and let each instance
obtain a new Ticket and lease to recover. Apply IdP V2 before DDC V8 consumers;
V8 is a breaking protocol migration, has both PostgreSQL and SQLite variants,
and must be rolled forward rather than by editing or reversing existing Flyway
files.

## Service Registry

The service identity is:

```text
env + namespace + serviceKind + serviceName + group + version + protocol
```

Supported `serviceKind` values are `HTTP_PROVIDER`, `RPC_PROVIDER`, and
`INTERNAL_GATEWAY`.
The registration carries `instanceId`, host, port, secure flag, metadata,
lease seconds, and heartbeat interval. Metadata is bounded and rejects reserved
or sensitive keys.

Direct unary gRPC services:

| Service | Operations |
|---|---|
| `DdcConfigRuntimeService` | register, heartbeat, offline, pull, publish ACK |
| `DdcServiceRegistryService` | register, heartbeat, deregister, instance snapshot, service catalog |
| `DdcManagementService` | config CRUD, publish/task operations, config-client, scope, and registry reads |

`DdcServiceRegistryClient` also exposes instance and catalog subscriptions.
Redis revisions and Pub/Sub notifications trigger reconciliation; expired
entries are removed from snapshots. Redis restart loses registry state by
design, after which clients register again with new leases.

## Synchronous Publish

V1 has one publish mode: `SYNC_ALL_ACK`.

The caller supplies a UUIDv7 `changeId`. The preparation transaction takes a
pessimistic lock on the configuration row, rejects another active task for the
same resource, updates the configuration version, and freezes the current Redis
lease targets as exact `instanceId + leaseId` pairs. The database task and row
conditions coordinate concurrent Admin processes for
`bizCode + env + appCode + resourceName`; the in-memory waiter is only a wake-up
optimization. Completion polling reads the shared task state, and startup recovery
marks stale `PENDING` or `PUBLISHING` tasks as `UNKNOWN` before another request retries.

An ACK is accepted only when all of these match:

- `changeId`;
- target `instanceId + leaseId`;
- target configuration version;
- resource SHA-256 checksum covering `resourceName + format + content`.

The publish call returns only after every target reports `SUCCESS`, or after a
terminal failure:

| Status | Meaning |
|---|---|
| `SUCCESS` | Every frozen target acknowledged successfully |
| `FAILED` | Preparation, dispatch, target validation, or target ACK failed |
| `TIMEOUT` | Not every target acknowledged before the deadline |
| `UNKNOWN` | Admin restarted while the task was active |

`POST /api/v1/ddc/publish-tasks/{changeId}/retry` retries `FAILED`, `TIMEOUT`,
or `UNKNOWN` tasks idempotently. Retry keeps the original targets and never
resnapshots currently live instances. If any original target lease has expired,
retry remains failed with a target-lease error.

Publish request:

```bash
curl -X POST \
  'http://localhost:18080/api/v1/ddc/configs/{configId}/publish?operator=admin' \
  -H 'Content-Type: application/json' \
  -d '{
    "changeId": "019c9f0d-7b9b-7e00-8000-000000000001",
    "content": "order:\n  rate-limit:\n    permits-per-second: 200\n",
    "expectedVersion": 1,
    "timeoutMs": 30000
  }'
```

The call is intentionally synchronous. Query the same `changeId` through
`GET /api/v1/ddc/publish-tasks/{changeId}` when a caller loses the response.

## RPC HMAC

HMAC protects every published DDC unary method when `signature-enabled` is true.
Required gRPC metadata is:

| Header | Value |
|---|---|
| `x-egon-ddc-access-key` | configured access key |
| `x-egon-ddc-timestamp` | Unix epoch milliseconds |
| `x-egon-ddc-nonce` | unique request nonce |
| `x-egon-ddc-content-sha256` | lowercase SHA-256 of deterministic protobuf bytes |
| `x-egon-ddc-signature` | HMAC-SHA256 of the canonical request |
| `x-egon-ddc-contract-version` | `v1` |

The canonical value is five newline-separated fields:

```text
v1
full-grpc-method-name
timestamp
nonce
content-sha256
```

The Admin checks the known method/operation mapping, contract version, clock skew,
access key, deterministic body digest, signature, nonce replay, client type, and
scope. Runtime, registry, and management clients use separate credentials.

## Configuration

Migration from the removed machine HTTP transport is intentionally breaking:

| Removed prefix | Removed leaf | Direct RPC replacement |
|---|---|---|
| `egon.cola.component.ddc.admin` | `endpoint` | `egon.cola.component.ddc.rpc.target` |
| `egon.cola.component.ddc.admin` | `tls.*` | `egon.cola.component.ddc.rpc.tls.*` |
| `egon.cola.component.ddc.admin` | `access-key` / `secret-key` | `egon.cola.component.ddc.rpc.auth.runtime.*` or `.registry.*` by capability |
| `gateway.admin.ddc` | `endpoint` and HMAC keys | `egon.cola.component.ddc.rpc.target` plus `.auth.management.*` |
| `egon.cola.component.ddc.admin.openapi` | `signature-enabled` / `credentials` | `egon.cola.component.ddc.admin.rpc.signature-enabled` / `.credentials` |

There is no compatibility alias. Credentials are environment-injected and runtime,
registry, and management use distinct access-key/secret pairs.

Business application:

```yaml
spring:
  config:
    import: ddc:application.yml

egon:
  cola:
    component:
      ddc:
        enabled: true
        biz-code: orders
        app-code: order-service
        env: dev
        namespace: default
        rpc:
          target: dns:///ddc-admin.example.internal:19080
          load-balancing-policy: round_robin
          tls:
            enabled: true
            development-plaintext: false
            certificate-chain-path: ${DDC_CLIENT_CERTIFICATE}
            private-key-path: ${DDC_CLIENT_PRIVATE_KEY}
            trust-certificate-collection-path: ${DDC_TRUST_CERTIFICATE}
          auth:
            runtime:
              access-key: ${DDC_RUNTIME_ACCESS_KEY}
              secret-key: ${DDC_RUNTIME_SECRET_KEY}
            registry:
              access-key: ${DDC_REGISTRY_ACCESS_KEY}
              secret-key: ${DDC_REGISTRY_SECRET_KEY}
        redis:
          mode: SINGLE
          nodes: []
          master-name:
          enabled: true
          host: 127.0.0.1
          port: 6379
          database: 0
        instance:
          lease-seconds: 30
          heartbeat-interval-seconds: 10
        registry:
          enabled: true
          reconcile-interval-seconds: 10
        consistency:
          fail-fast: true
```

Use `optional:ddc:application.yml` when absence of the remote document is allowed.
DDC contributes one PropertySource above local ConfigData and below Spring Boot's
higher-priority command-line and system sources; it does not merge local and remote
documents. The Admin accepts only one Map-root, single-document YAML resource named
`application.yml` or `application.yaml` per `bizCode + env + appCode`; the resource
name in `spring.config.import` must match the `resourceName` stored by Admin. Remote YAML containing
`egon.cola.component.ddc.*`, `spring.config.*`, or Spring profile-control keys is
rejected as a whole, so DDC connection and bootstrap controls remain local.

Configuration parsing is an extension point implemented by
`DdcConfigFormatStrategy` and `DdcConfigFormatStrategyRegistry`. YAML is the only
built-in strategy and delegates parsing to Spring Boot's `YamlPropertySourceLoader`;
JSON, Properties, TOML, and other formats have no compatibility implementation and
are rejected by the registry.

Production Admin:

```yaml
server:
  port: 18080

spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/egon_ddc
    username: ${DDC_DB_USERNAME}
    password: ${DDC_DB_PASSWORD}
  flyway:
    locations: classpath:db/postgresql

egon:
  cola:
    component:
      ddc:
        enabled: false
        admin:
          transport-security:
            mode: DEVELOPMENT_PLAINTEXT
          redis:
            mode: SINGLE
            nodes: []
            master-name:
            enabled: true
            host: 127.0.0.1
            port: 6379
            database: 0
          lease:
            minimum-seconds: 5
            maximum-seconds: 300
          rpc:
            signature-enabled: true
            credentials:
              - credential-id: runtime
                access-key: ${DDC_RUNTIME_ACCESS_KEY}
                secret: ${DDC_RUNTIME_SECRET_KEY}
                client-type: SDK
                app-code-patterns: [gateway-engine-*]
                env-patterns: [local]
                biz-code-patterns: [infra]
                namespace-patterns: [default]
                allowed-operations: [SDK_REGISTER, SDK_HEARTBEAT,
                  SDK_OFFLINE, CONFIG_PULL, PUBLISH_ACK]
              - credential-id: registry
                access-key: ${DDC_REGISTRY_ACCESS_KEY}
                secret: ${DDC_REGISTRY_SECRET_KEY}
                client-type: REGISTRY
                app-code-patterns: [gateway-*]
                env-patterns: [local]
                biz-code-patterns: [infra]
                namespace-patterns: [default]
                allowed-operations: [REGISTRY_REGISTER,
                  REGISTRY_HEARTBEAT, REGISTRY_DEREGISTER, REGISTRY_READ]
              - credential-id: management
                access-key: ${DDC_MANAGEMENT_ACCESS_KEY}
                secret: ${DDC_MANAGEMENT_SECRET_KEY}
                client-type: MANAGEMENT
                app-code-patterns: [gateway-engine-*]
                env-patterns: [local]
                biz-code-patterns: [infra]
                namespace-patterns: [default]
                allowed-operations: [MANAGEMENT_CONFIG_READ,
                  MANAGEMENT_CONFIG_WRITE, MANAGEMENT_PUBLISH,
                  MANAGEMENT_TASK_READ, MANAGEMENT_TASK_RETRY,
                  MANAGEMENT_INSTANCE_READ, MANAGEMENT_SCOPE_READ,
                  MANAGEMENT_REGISTRY_READ,
                  MANAGEMENT_CATALOG_READ]
          publish:
            dispatch-timeout-ms: 5000
            default-timeout-ms: 30000
            max-timeout-ms: 60000
            scan-interval-ms: 1000
            completion-poll-interval-ms: 100
            recovery-stale-ms: 120000
      rpc:
        enabled: true
        provider:
          enabled: true
          port: 19080
          registration-mode: DISABLED
        consumer:
          enabled: false
        tls:
          enabled: true
          development-plaintext: false
          certificate-chain-path: ${DDC_SERVER_CERTIFICATE}
          private-key-path: ${DDC_SERVER_PRIVATE_KEY}
          trust-certificate-collection-path: ${DDC_TRUST_CERTIFICATE}
```

The `test` profile uses SQLite with `create-drop` and disables Flyway and the
Admin Redis connection. It is not the production storage topology.

## Build and Validation

```bash
./mvnw -B -ntp \
  -pl :egon-cola-component-rpc-ddc-adapter,:egon-cola-platform-dynamic-config-center-admin,:egon-cola-platform-dynamic-config-center-test \
  -am clean test

./mvnw -B -ntp \
  -pl :egon-cola-platform-dynamic-config-center-admin,:egon-cola-component-rpc-ddc-adapter \
  -am package -DskipTests
```

## Explicit Boundaries

For the complete DDC + Gateway + RPC startup order, credentials, lease drills, and
runtime evidence, use the [developer integration runbook](../egon-cola-platform-gateway/docs/developer-integration.md).

- no Raft, leader election, consensus log, or membership protocol;
- multi-Admin operation requires shared PostgreSQL and Redis; the platform does not provision database or Redis HA;
- DDC uses a direct logical RPC target with client-side or external round-robin; it does not register itself, discover itself, require sticky sessions, or stream config over gRPC;
- no distributed consensus or general-purpose distributed lock service;
- no embedded Redis and no database-backed service registry;
- no embedded Admin UI or account system; the standalone Admin Web uses the
  platform identity and authorization integration, and MySQL compatibility is
  not a target;
- no asynchronous, quorum, or partial-success publish mode in V1.
