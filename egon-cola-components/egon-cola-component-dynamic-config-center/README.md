# Egon COLA Dynamic Config Center

[English](README.md) | [中文](README.zh-CN.md)

## Scope

`egon-cola-component-dynamic-config-center` provides a dynamic-configuration SDK
with typed management APIs, a standalone Admin application, and a Redis-backed
service registry for RPC Providers and internal Gateways.

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
Configuration Clients ──HTTP/HMAC──┐
RPC Providers ──────────HTTP/HMAC──┼──> one or more DDC Admins ──> PostgreSQL
Internal Gateway ───────HTTP/HMAC──┘              │
                                                  └──> shared Redis
Configuration Clients <──── Redis Pub/Sub ────────┘
Registry Subscribers  <──── Redis Pub/Sub ────────┘
```

The Admin processes are the only management and lease API endpoints. They share
PostgreSQL and Redis; no Admin-local data is authoritative for a completed publish.
Redis contains
configuration cache, publish notifications, live configuration-client leases,
and service-registry leases. PostgreSQL stores configuration, version, publish,
ACK, operation, and configuration-client projection data.

## Modules

| Module | Responsibility |
|---|---|
| `egon-cola-component-dynamic-config-center-starter` | The only consumer SDK: `@DdcValue`, typed management APIs, startup synchronization, refresh, ACK, CONFIG_CLIENT leases, HMAC, and service-registry contracts |
| `egon-cola-component-dynamic-config-center-admin` | Standalone REST Admin, PostgreSQL persistence, Redis cache and leases, registry APIs, and synchronous publish state machine |
| `egon-cola-component-dynamic-config-center-test` | Starter-only sample and black-box consumer verification; it has no Admin dependency |

Applications add only the Starter. `egon.cola.component.ddc.enabled=true` explicitly
starts the `CONFIG_CLIENT` registration, default-report, pull, Redis subscription,
heartbeat, and shutdown-offline lifecycle. `egon.cola.component.ddc.registry.enabled=true`
independently enables RPC/Gateway service registration; those `RPC_PROVIDER`,
`HTTP_PROVIDER`, and `INTERNAL_GATEWAY` leases are not configuration-client registrations. Every enabled
remote path must explicitly configure the Admin Endpoint, matching HMAC credentials,
and Redis topology. With `redis.enabled=false`, no registration, pull, subscription,
heartbeat, or ACK runs. Production multi-Admin access must use an external DNS name,
VIP, or load balancer; Starter does not discover Admin processes.

```xml
<dependency>
    <groupId>top.egon</groupId>
    <artifactId>egon-cola-component-dynamic-config-center-starter</artifactId>
</dependency>
```

## Configuration Client Lifecycle

`DdcRuntimeCoordinator` starts only after the Redis subscription is active and
executes this order:

1. register the configuration client and receive a new `leaseId`;
2. report annotation defaults;
3. pull and apply the complete configuration snapshot;
4. enter `READY`;
5. heartbeat on the configured interval;
6. actively take the current lease offline during shutdown.

Each registration replaces the old lease. Heartbeat and offline operations
atomically match `instanceId + leaseId`; a stale lease cannot renew or delete
the replacement lease. When a current lease is missing or mismatched, the SDK
registers again and repeats initial synchronization.

```java
@DdcValue("rateLimit:100")
private volatile Integer rateLimit;

@DdcValue(value = "", key = "downgradeSwitch",
        defaultValue = "false", type = Boolean.class)
private volatile Boolean downgradeSwitch;
```

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

## Service Registry

The service identity is:

```text
env + namespace + serviceKind + serviceName + group + version + protocol
```

Supported `serviceKind` values are `RPC_PROVIDER` and `INTERNAL_GATEWAY`.
The registration carries `instanceId`, host, port, secure flag, metadata,
lease seconds, and heartbeat interval. Metadata is bounded and rejects reserved
or sensitive keys.

OpenAPI endpoints:

| Method and path | Purpose |
|---|---|
| `POST /api/v1/ddc/openapi/registry/instances/register` | Register and receive a new lease |
| `POST /api/v1/ddc/openapi/registry/instances/heartbeat` | Renew only the matching current lease |
| `POST /api/v1/ddc/openapi/registry/instances/deregister` | Remove only the matching current lease |
| `GET /api/v1/ddc/openapi/registry/instances` | Read a stable live-instance snapshot for one service key |
| `GET /api/v1/ddc/openapi/registry/services` | Read the live service catalog |

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
`appCode + env + namespace + configKey`; the in-memory waiter is only a wake-up
optimization. Completion polling reads the shared task state, and startup recovery
marks stale `PENDING` or `PUBLISHING` tasks as `UNKNOWN` before another request retries.

An ACK is accepted only when all of these match:

- `changeId`;
- target `instanceId + leaseId`;
- target configuration version;
- content SHA-256 checksum.

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
    "configValue": "200",
    "expectedVersion": 1,
    "timeoutMs": 30000
  }'
```

The call is intentionally synchronous. Query the same `changeId` through
`GET /api/v1/ddc/publish-tasks/{changeId}` when a caller loses the response.

## OpenAPI HMAC

HMAC protects every path under `/api/v1/ddc/openapi/` when
`signature-enabled` is true. Required headers are:

| Header | Value |
|---|---|
| `X-DDC-Access-Key` | configured access key |
| `X-DDC-Timestamp` | Unix epoch milliseconds |
| `X-DDC-Nonce` | unique request nonce |
| `X-DDC-Content-SHA256` | lowercase SHA-256 hex of the exact request body |
| `X-DDC-Signature` | HMAC-SHA256 hex of the canonical request |

The canonical value is six newline-separated fields:

```text
UPPERCASE_HTTP_METHOD
request-path
canonical-query
timestamp
nonce
content-sha256
```

The canonical query percent-encodes UTF-8 with RFC 3986 unreserved characters,
sorts by encoded key and value, and preserves repeated parameters. The Admin
checks clock skew, access key, body digest, signature, and nonce replay.

## Configuration

Business application:

```yaml
egon:
  cola:
    component:
      ddc:
        enabled: true
        app-code: order-service
        env: dev
        namespace: default
        admin:
          endpoint: http://localhost:18080
          signature-enabled: true
          access-key: ${DDC_ACCESS_KEY}
          secret-key: ${DDC_SECRET_KEY}
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
          openapi:
            signature-enabled: true
            credentials:
              - credential-id: gateway-engine
                access-key: ${DDC_ACCESS_KEY}
                secret: ${DDC_SECRET_KEY}
                client-type: "*"
                app-code-patterns: [gateway-engine-*]
                env-patterns: [local]
                namespace-patterns: [default]
                allowed-operations: [SDK_REGISTER, SDK_HEARTBEAT,
                  SDK_OFFLINE, CONFIG_PULL, CONFIG_VALUE, PUBLISH_ACK,
                  DEFAULTS_REPORT, REGISTRY_REGISTER, REGISTRY_HEARTBEAT,
                  REGISTRY_DEREGISTER, REGISTRY_READ]
          publish:
            dispatch-timeout-ms: 5000
            default-timeout-ms: 30000
            max-timeout-ms: 60000
            scan-interval-ms: 1000
            completion-poll-interval-ms: 100
            recovery-stale-ms: 120000
```

The `test` profile uses SQLite with `create-drop` and disables Flyway and the
Admin Redis connection. It is not the production storage topology.

## Build and Validation

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test \
  -am clean test

./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter \
  -am package -DskipTests
```

## Explicit Boundaries

For the complete DDC + Gateway + RPC startup order, credentials, lease drills, and
runtime evidence, use the [developer integration runbook](../egon-cola-component-gateway/docs/developer-integration.md).

- no Raft, leader election, consensus log, or membership protocol;
- multi-Admin operation requires shared PostgreSQL and Redis; the component does not provision database or Redis HA;
- no distributed consensus or general-purpose distributed lock service;
- no embedded Redis and no database-backed service registry;
- no UI, account system, RBAC, or MySQL compatibility target;
- no asynchronous, quorum, or partial-success publish mode in V1.
