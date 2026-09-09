# Egon COLA Tianshu (Dynamic Config Center)

[English](README.md) | [中文](README.zh-CN.md)

## Scope

`egon-cola-tianshu` provides a Spring Boot ConfigData SDK
for one YAML business-configuration document, typed management APIs, a standalone
Admin application, and a Redis-backed service registry for RPC Providers and
internal Gateways.

The Maven modules use the `egon-cola-xingyuan-*` prefix. The Starter Java API is
organized by domain and does not retain forwarding types for its former technical
packages. The external `egon.cola.component.tianshu` configuration namespace remains
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
RPC Providers ────────direct gRPC/HMAC──┼──> one logical Tianshu target ──> Admin set ──> PostgreSQL
Internal Yuheng ─────direct gRPC/HMAC──┘                                      │
                                                                                 └──> shared Redis
Configuration Clients <──── Redis Pub/Sub ────────┘
Registry Subscribers  <──── Redis Pub/Sub ────────┘
```

The Admin processes are the only machine control-plane RPC providers. Clients use
a locally configured `dns:///`, VIP, or load-balancer target and never bootstrap
Tianshu through Tianshu service discovery. Admin HTTP remains for human management APIs and
Actuator health only. The Admin processes share
PostgreSQL and Redis; no Admin-local data is authoritative for a completed publish.
Redis contains
configuration cache, publish notifications, live configuration-client leases,
and service-registry leases. PostgreSQL stores configuration, version, publish,
ACK, operation, and configuration-client projection data.

## Modules

| Module | Responsibility |
|---|---|
| `egon-cola-tianshu-starter` | Transport-neutral SDK runtime and ports: ConfigData, `@DdcValue`, selective refresh, ACK, leases, management and registry contracts |
| `egon-cola-tianshu-http-registration-starter` | Spring HTTP service registration, Tianshu lease heartbeat/recovery, and registration metadata contributor SPI |
| `egon-cola-component-rpc-tianshu-adapter` | Composition adapter under `components/rpc`: protobuf contracts, direct gRPC clients/providers, HMAC metadata, and Spring Boot wiring |
| `egon-cola-tianshu-admin` | Human REST Admin plus direct gRPC facades, PostgreSQL persistence, Redis cache/leases, and synchronous publish state machine |
| `egon-cola-tianshu-admin-web` | Standalone management console (React + antd + Vite, pure Node project outside the Maven reactor); build and deployment instructions live in `egon-cola-tianshu-admin-web/README.md` |
| `egon-cola-tianshu-test` | Starter samples, black-box consumer verification, and cross-boundary identity/lease lifecycle acceptance tests |

The Admin web UI has been extracted from the jar (`/tianshu-admin` is no longer served
by Admin). The console deploys as its own container, points at Admin via
`TIANSHU_ADMIN_API_BASE_URL`, and proxies `/api` same-origin through its static
server, so Admin needs no CORS configuration.

## Admin Page Queries

Tianshu Admin exposes additive server-side page queries for human management:

```text
GET /api/v1/tianshu/bizs/page
GET /api/v1/tianshu/namespaces/page
GET /api/v1/tianshu/envs/page
GET /api/v1/tianshu/apps/page
GET /api/v1/tianshu/namespace-env-app-bindings/page
GET /api/v1/tianshu/configs/page
GET /api/v1/tianshu/configs/{id}/versions/page
GET /api/v1/tianshu/publish-tasks/page
GET /api/v1/tianshu/instances/page
GET /api/v1/tianshu/cache/check/page
GET /api/v1/tianshu/registry/services/page
GET /api/v1/tianshu/registry/instances/page
```

For example:

```text
GET /api/v1/tianshu/bizs/page?pageNo=1&pageSize=10
success -> PageResultRecord { records, page }
failure -> existing ResultRecord error envelope
legacy list/catalog/snapshot endpoints remain available
```

Page numbers start at 1. These REST endpoints are for Admin Web and other human
management clients. Starter and RPC machine clients continue to consume complete
catalogs and snapshots rather than the Admin page contract.

## Starter Package Layout

```text
top.egon.cola.component.tianshu
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
separate domain facades. The RPC-Tianshu Adapter implements them over three unary gRPC
services. The base starter does not depend on RPC. The HTTP registration starter is
an application composition starter and therefore includes the RPC-Tianshu Adapter used
to send registry operations to Tianshu. HTTP registration properties use the
`egon.cola.component.tianshu.registry.http` namespace.

## Operations Endpoints

Tianshu Admin exposes `GET /actuator/health/readiness` for startup and readiness
checks. `GET /actuator/info` exposes the application name and the Maven-filtered
component version under `app.name` and `app.version`.

Executable applications add the RPC-Tianshu Adapter and import `tianshu:application.yml` through
`spring.config.import`. `egon.cola.component.tianshu.enabled=true` loads the remote YAML
during ConfigData processing, then starts the `CONFIG_CLIENT` registration, pull,
Redis subscription, heartbeat, and shutdown-offline lifecycle.
`egon.cola.component.tianshu.registry.enabled=true` independently enables RPC/Yuheng
service registration; those `RPC_PROVIDER`,
`HTTP_PROVIDER`, and `INTERNAL_GATEWAY` leases are not configuration-client registrations. Every enabled
remote path must locally configure the direct RPC target, matching least-privilege
HMAC credentials, and Redis topology. With `redis.enabled=false`, no registration, pull, subscription,
heartbeat, or ACK runs. Production multi-Admin access must use an external DNS name,
VIP, or HTTP/2-capable load balancer with `round_robin`; Tianshu never discovers its own
Admin processes and no Tianshu machine HTTP compatibility endpoint exists.

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-rpc-tianshu-adapter</artifactId>
    <version>5.4.0</version>
</dependency>
```

## Trace Propagation

The Starter and RPC-Tianshu Adapter use `egon-cola-component-common-trace` in gRPC
facade calls, heartbeat, pull, ACK retry, Redis topic callbacks, and lease recovery
tasks. Tianshu calls triggered by a business request inherit the
current `traceId` and create a child span. Background tasks without an upstream
trace open a fresh `TraceContext` for each logical operation and restore the
worker thread MDC afterwards. Outbound requests write only `traceparent`,
`tracestate`, and `x-egon-request-id`; they do not write `x-egon-trace-id`.

The Spring MVC Admin application depends on
`egon-cola-component-common-trace-spring-boot-starter` directly and no longer keeps a
Tianshu-specific trace filter.

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

Runtime publication atomically replaces the Tianshu PropertySource and computes YAML
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

### Tianquan-Shoubing SERVICE token and lease boundary

Every configuration-client, HTTP Provider, RPC Provider, and internal-Yuheng
registration or heartbeat obtains a fresh Tianquan-Shoubing SERVICE token through the standard
Spring OAuth2 Client `client_credentials` flow. The grant targets the Tianshu
Resource with `grantContext: PLATFORM` and the least-privilege registration
scope. Tianshu verifies the signed token, exact audience, source/application
identity, scope, instance binding, nonce/replay state, and expiry; it does not
accept a second registration ticket or RPC credential.

An enabled Resource is approved at its logical `bizCode + appCode + env` triple,
so instances do not require individual catalog approval. Lease expiry is capped
by the SERVICE token validity boundary, raw tokens are never stored in registry
or audit state, and a missing/expired token cannot create or extend a lease. If
Tianquan-Shoubing is unavailable, an existing lease runs only until its current token-bound
expiry; the runtime enters recovery and becomes not ready when it cannot renew
safely.

Disabling a Resource removes only leases whose logical triple matches the
disabled Resource. Re-enable the Resource, restore the required Tianquan-Shoubing grant, and
let each instance obtain a fresh SERVICE token and lease. Apply the Tianquan-Shoubing V5 and
compatible Tianshu release together; this is a breaking protocol migration and
existing Flyway files must not be edited or reversed.

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

`POST /api/v1/tianshu/publish-tasks/{changeId}/retry` retries `FAILED`, `TIMEOUT`,
or `UNKNOWN` tasks idempotently. Retry keeps the original targets and never
resnapshots currently live instances. If any original target lease has expired,
retry remains failed with a target-lease error.

Publish request:

```bash
curl -X POST \
  'http://localhost:18080/api/v1/tianshu/configs/{configId}/publish?operator=admin' \
  -H 'Content-Type: application/json' \
  -d '{
    "changeId": "019c9f0d-7b9b-7e00-8000-000000000001",
    "content": "order:\n  rate-limit:\n    permits-per-second: 200\n",
    "expectedVersion": 1,
    "timeoutMs": 30000
  }'
```

The call is intentionally synchronous. Query the same `changeId` through
`GET /api/v1/tianshu/publish-tasks/{changeId}` when a caller loses the response.

## RPC HMAC

HMAC protects every published Tianshu unary method when `signature-enabled` is true.
Required gRPC metadata is:

| Header | Value |
|---|---|
| `x-egon-tianshu-access-key` | configured access key |
| `x-egon-tianshu-timestamp` | Unix epoch milliseconds |
| `x-egon-tianshu-nonce` | unique request nonce |
| `x-egon-tianshu-content-sha256` | lowercase SHA-256 of deterministic protobuf bytes |
| `x-egon-tianshu-signature` | HMAC-SHA256 of the canonical request |
| `x-egon-tianshu-contract-version` | `v1` |

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
| `egon.cola.component.tianshu.admin` | `endpoint` | `egon.cola.component.tianshu.rpc.target` |
| `egon.cola.component.tianshu.admin` | `tls.*` | `egon.cola.component.tianshu.rpc.tls.*` |
| `egon.cola.component.tianshu.admin` | `access-key` / `secret-key` | `egon.cola.component.tianshu.rpc.auth.runtime.*` or `.registry.*` by capability |
| `yuheng.admin.tianshu` | `endpoint` and HMAC keys | `egon.cola.component.tianshu.rpc.target` plus `.auth.management.*` |
| `egon.cola.component.tianshu.admin.openapi` | `signature-enabled` / `credentials` | `egon.cola.component.tianshu.admin.rpc.signature-enabled` / `.credentials` |

There is no compatibility alias. Credentials are environment-injected and runtime,
registry, and management use distinct access-key/secret pairs.

Business application:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          egon-tianquan-shoubing:
            client-id: ${EGON_TIANQUAN_SHOUBING_APP_KEY}
            client-secret: ${EGON_TIANQUAN_SHOUBING_APP_SECRET}
            authorization-grant-type: client_credentials
            client-authentication-method: client_secret_basic
        provider:
          egon-tianquan-shoubing:
            token-uri: ${EGON_TIANQUAN_SHOUBING_TOKEN_URI}
  config:
    import: tianshu:application.yml

egon:
  cola:
    xingyuan:
      tianquan-shoubing:
        service-client:
          app-id: ${EGON_TIANQUAN_SHOUBING_APP_ID}
    component:
      tianshu:
        enabled: true
        biz-code: orders
        app-code: order-service
        env: dev
        namespace: default
        rpc:
          target: dns:///tianshu-admin.example.internal:19080
          load-balancing-policy: round_robin
          tls:
            enabled: true
            development-plaintext: false
            certificate-chain-path: ${TIANSHU_CLIENT_CERTIFICATE}
            private-key-path: ${TIANSHU_CLIENT_PRIVATE_KEY}
            trust-certificate-collection-path: ${TIANSHU_TRUST_CERTIFICATE}
          auth:
            runtime:
              access-key: ${TIANSHU_RUNTIME_ACCESS_KEY}
              secret-key: ${TIANSHU_RUNTIME_SECRET_KEY}
            registry:
              access-key: ${TIANSHU_REGISTRY_ACCESS_KEY}
              secret-key: ${TIANSHU_REGISTRY_SECRET_KEY}
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

The TLS `private-key-path` above is a transport certificate key only; it is not
an OAuth client credential. OAuth client identity is the Tianquan-Shoubing-administered
`appId`/`client_id` and one-time Secret. Keep all Secret values outside this
document and follow the
[cutover runbook](../../docs/runbooks/unified-identity-oauth-client-tenant-cutover.md)
for release ordering and restore evidence.

Use `optional:tianshu:application.yml` when absence of the remote document is allowed.
Tianshu contributes one PropertySource above local ConfigData and below Spring Boot's
higher-priority command-line and system sources; it does not merge local and remote
documents. The Admin accepts only one Map-root, single-document YAML resource named
`application.yml` or `application.yaml` per `bizCode + env + appCode`; the resource
name in `spring.config.import` must match the `resourceName` stored by Admin. Remote YAML containing
`egon.cola.component.tianshu.*`, `spring.config.*`, or Spring profile-control keys is
rejected as a whole, so Tianshu connection and bootstrap controls remain local.

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
    url: jdbc:postgresql://127.0.0.1:5432/egon_tianshu
    username: ${TIANSHU_DB_USERNAME}
    password: ${TIANSHU_DB_PASSWORD}
  flyway:
    locations: classpath:db/postgresql

egon:
  cola:
    component:
      tianshu:
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
                access-key: ${TIANSHU_RUNTIME_ACCESS_KEY}
                secret: ${TIANSHU_RUNTIME_SECRET_KEY}
                client-type: SDK
                app-code-patterns: [yuheng-biz-gateway-*]
                env-patterns: [local]
                biz-code-patterns: [infra]
                namespace-patterns: [default]
                allowed-operations: [SDK_REGISTER, SDK_HEARTBEAT,
                  SDK_OFFLINE, CONFIG_PULL, PUBLISH_ACK]
              - credential-id: registry
                access-key: ${TIANSHU_REGISTRY_ACCESS_KEY}
                secret: ${TIANSHU_REGISTRY_SECRET_KEY}
                client-type: REGISTRY
                app-code-patterns: [yuheng-*]
                env-patterns: [local]
                biz-code-patterns: [infra]
                namespace-patterns: [default]
                allowed-operations: [REGISTRY_REGISTER,
                  REGISTRY_HEARTBEAT, REGISTRY_DEREGISTER, REGISTRY_READ]
              - credential-id: management
                access-key: ${TIANSHU_MANAGEMENT_ACCESS_KEY}
                secret: ${TIANSHU_MANAGEMENT_SECRET_KEY}
                client-type: MANAGEMENT
                app-code-patterns: [yuheng-biz-gateway-*]
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
          certificate-chain-path: ${TIANSHU_SERVER_CERTIFICATE}
          private-key-path: ${TIANSHU_SERVER_PRIVATE_KEY}
          trust-certificate-collection-path: ${TIANSHU_TRUST_CERTIFICATE}
```

The `test` profile uses SQLite with `create-drop` and disables Flyway and the
Admin Redis connection. It is not the production storage topology.

## Build and Validation

```bash
./mvnw -B -ntp \
  -pl :egon-cola-component-rpc-tianshu-adapter,:egon-cola-tianshu-admin,:egon-cola-tianshu-test \
  -am clean test

./mvnw -B -ntp \
  -pl :egon-cola-tianshu-admin,:egon-cola-component-rpc-tianshu-adapter \
  -am package -DskipTests
```

## Explicit Boundaries

For the complete Tianshu + Yuheng + RPC startup order, credentials, lease drills, and
runtime evidence, use the [developer integration runbook](../egon-cola-yuheng/docs/developer-integration.md).

- no Raft, leader election, consensus log, or membership protocol;
- multi-Admin operation requires shared PostgreSQL and Redis; the xingyuan does not provision database or Redis HA;
- Tianshu uses a direct logical RPC target with client-side or external round-robin; it does not register itself, discover itself, require sticky sessions, or stream config over gRPC;
- no distributed consensus or general-purpose distributed lock service;
- no embedded Redis and no database-backed service registry;
- no embedded Admin UI or account system; the standalone Admin Web uses the
  xingyuan identity and authorization integration, and MySQL compatibility is
  not a target;
- no asynchronous, quorum, or partial-success publish mode in V1.
