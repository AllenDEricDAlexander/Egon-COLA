# Yuheng Local Deployment and Runtime Boundaries

[中文](README.zh-CN.md) | [Yuheng overview](../README.md)

This directory provides deployment examples for Yuheng Engine, Yuheng Admin, Admin Web,
and their local dependencies. It is not a production HA solution and does not manage Nginx
node load balancing or dynamic configuration. The Admin remains one logical control plane; the fixed API_RPC and MCP roles have separate executables.

## Build prerequisites

Build the four executable artifacts from the repository root:

```bash
./mvnw -B -ntp \
  -pl egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin,egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin,egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway,egon-cola-xingyuan/egon-cola-yuheng/yuheng-mcp-gateway \
  -am package -DskipTests
```

Copy `.env.example` to `.env` and fill it locally with separate random Tianshu runtime,
registry, and management credentials plus a Base64-encoded 32-byte master key. Do
not reuse one Tianshu secret across capability profiles and do not commit `.env`. The
operator may then run:

```bash
docker compose --env-file .env -f compose.yml build
docker compose --env-file .env -f compose.yml up -d
```

This documentation update does not start the stack automatically.

For the complete MVC, WebFlux, RPC Provider, RPC Consumer, and dual-Engine demo,
use the [developer integration runbook](../docs/developer-integration.md). The command
facade is `./scripts/demo.sh`; `down` preserves data and the explicitly destructive
`purge` command is restricted to a marked local demo project.

## Ports and persistence

| Service | Local port | Purpose |
|---|---:|---|
| Tianshu Admin HTTP | 18070 | Human management API and Actuator readiness |
| Tianshu Admin gRPC | 19080 | Direct runtime, registry, and management facades |
| Yuheng Admin | 18080 | Management API and health endpoints |
| API_RPC 1 PUBLIC (direct) | 18091 | External HTTP data plane |
| Engine 1 INTERNAL | 18082 | Internal HTTP data plane |
| Engine 1 Management | 18083 | Actuator |
| Engine 1 RPC Slot | 19090 | Egon RPC internal Yuheng |
| API_RPC 2 PUBLIC | 18181 | Second external HTTP data plane |
| Engine 2 INTERNAL | 18182 | Second internal HTTP data plane |
| Engine 2 Management | 18183 | Second Actuator |
| Engine 2 RPC Slot | 19190 | Second Egon RPC internal Yuheng |
| Stable data-plane proxy | 18081 | Existing API/MCP Host/Path entry |
| MCP 1 Data / Management | 18084 / 18085 | Loopback diagnostics and independent readiness |
| MCP 2 Data / Management | 18184 / 18185 | Second MCP replica |
| Demo MVC / WebFlux | 18094 / 18095 | Avoid MCP default port collisions |
| Admin Web | 18090 | React management page |

Persist each Engine's LKG directory independently. Tianshu Redis and the distributed rate-limit
Redis must use separate instances and data volumes. Initialize two PostgreSQL databases so
that Tianshu and Yuheng Admin Flyway histories cannot interfere with each other.

## Health and release order

Recommended checks:

```text
Tianshu Admin  GET /actuator/health/readiness
Admin      GET /actuator/health/liveness
Admin      GET /actuator/health/readiness
Engine     GET :18083/actuator/health/liveness
Engine     GET :18083/actuator/health/readiness
Admin Web  GET /healthz
```

An Engine process being alive does not mean that it is business-ready. For a first deployment,
deploy Tianshu Admin with its gRPC provider on `19080` first, verify HTTP readiness, then
start Tianshu-dependent Admin/Provider/Engine consumers. Wait for the Engine to register
as a Config Client, and then have Admin
publish the first valid Rule Release. An Engine should receive traffic only after its listener,
valid rules, and required Providers are ready.

## Automated acceptance

The fast gate does not start external processes; it covers Java unit/component tests, Admin Web
type checking, Vitest, ESLint, and the production build. The real-topology gate starts real Tianshu,
Admin, two API_RPC replicas, two MCP replicas, an HTTP Provider, an RPC Provider, and an RPC Consumer through the process
harness. Infrastructure uses Testcontainers by default:

```bash
./mvnw -B -ntp \
  -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite \
  -am -Pyuheng-live verify
```

For a host-only run, put `initdb`, `postgres`, and `redis-server` on `PATH` and select the local
backend. It creates temporary PostgreSQL and Redis instances on random ports and embeds a
single-node KRaft broker; it does not use existing database or Redis state:

```bash
./mvnw -B -ntp \
  -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite \
  -am -Pyuheng-live -Dgateway.live.infrastructure=local verify
```

The test verifies interface-definition reporting, rule
publication, registration and readiness of both roles, HTTP/RPC forwarding, load balancing
across two Providers, Provider removal, rate limiting, and Kafka Trace projection. Logs and
redacted process parameters are written to `target/yuheng-process-it`.

Release and shutdown order:

```text
Start: PostgreSQL/Redis/Kafka → Tianshu → Admin → Provider → Engine → Admin Web
Stop:  remove Engine traffic → bounded Engine drain → Provider → Admin → Tianshu → infrastructure
```

Compose reserves 30 seconds for graceful termination. A Kafka failure must not change the
business response, but dropped/failed events must be visible through metrics. When Tianshu is
temporarily unavailable, a running Engine may continue using valid in-memory state and LKG;
a restarted node with a validated LKG may serve degraded; it must not claim current-release consistency until the target version is acknowledged.

## Control-plane HA

`compose.ha.yml` adds a second Tianshu Admin, a second Yuheng Admin, and an HAProxy with
separate TCP frontends for human Tianshu HTTP (`18070`), Tianshu HTTP/2 gRPC (`19080`), and
Yuheng Admin HTTP (`18080`) on top of shared PostgreSQL, Tianshu Redis, and Kafka. It does not introduce Raft or
change the business Yuheng boundary. Tianshu publish consistency is still provided by PostgreSQL
row locks, conditional version updates, and persistent publish tasks; Redis provides cache,
Registry, and notification functions. Tianshu Admin can connect to production Redis with:

```text
EGON_COLA_COMPONENT_TIANSHU_ADMIN_REDIS_MODE=SENTINEL|CLUSTER
EGON_COLA_COMPONENT_TIANSHU_ADMIN_REDIS_NODES[0]=redis://redis-1:26379
EGON_COLA_COMPONENT_TIANSHU_ADMIN_REDIS_MASTER_NAME=tianshu-master
```

The HA example is started by the operator:

```bash
docker compose --env-file .env \
  -f compose.yml -f compose.ha.yml --profile ha config
docker compose --env-file .env \
  -f compose.yml -f compose.ha.yml --profile ha up -d
```

Proxy ports `18270`, `19280`, and `18280` expose Tianshu human HTTP, Tianshu gRPC, and
Yuheng Admin HTTP respectively. Tianshu clients use the logical target
`dns:///control-plane-proxy:19080` and `round_robin`; they do not discover Tianshu through
Tianshu. Removing either Admin container causes the TCP health check to remove that node.
The other instance continues reading the same PostgreSQL publish task and shared Redis
state. No sticky session, leader election, or Admin-local authoritative state is required.

RPC Yuheng Slots use the Tianshu `INTERNAL_GATEWAY` instance set in the same way. The Consumer keeps
unchanged channels, adds channels for new Engines, and performs bounded drain for Engines that
are going offline; it selects endpoints with round robin. Only `UNAVAILABLE` at the Yuheng
connection stage is retried on another node within the total deadline. Provider-stage failures
are not invoked again.

## TLS and mTLS

Production mode does not accept implicit plaintext. PUBLIC HTTP may use one-way TLS; INTERNAL
HTTP, RPC Slots, direct Tianshu RPC, and Yuheng Admin management can require mTLS. Certificates,
private keys, and trust chains are injected only through read-only file paths. There is no
skip-SAN/authority-validation or Trust-All switch. Local plaintext must explicitly set
`development-plaintext=true` or `transport-security.mode=DEVELOPMENT_PLAINTEXT`.

`compose.mtls.yml` demonstrates PEM-file injection. `${YUHENG_TLS_DIRECTORY}` must contain at
least:

```text
ca.crt
tianshu-admin.crt / tianshu-admin.key
tianshu-admin-2.crt / tianshu-admin-2.key
yuheng-admin.crt / yuheng-admin.key
yuheng-admin-2.crt / yuheng-admin-2.key
yuheng-admin-web.crt / yuheng-admin-web.key
yuheng-biz-gateway.crt / yuheng-biz-gateway.key
yuheng-biz-gateway-2.crt / yuheng-biz-gateway-2.key
yuheng-mcp-gateway.crt / yuheng-mcp-gateway.key
yuheng-mcp-gateway-2.crt / yuheng-mcp-gateway-2.key
yuheng-data-plane-proxy.pem (certificate chain and private key)
```

Private keys must be unencrypted PKCS#8 PEM. Certificate SANs must cover actual connection
names. With HA and mTLS, both Tianshu gRPC server certificates must cover the logical
`control-plane-proxy` authority because HAProxy uses TCP passthrough. Validate the configuration with:

```bash
docker compose --env-file .env \
  -f compose.yml -f compose.mtls.yml config
docker compose --env-file .env \
  -f compose.yml -f compose.ha.yml -f compose.mtls.yml \
  -f compose.ha-mtls.yml --profile ha config
```

Spring SSL Bundles use `reload-on-update=true`, so Tianshu Admin and Yuheng Admin watch for PEM
file updates. Actuator exposes `ssl.chain.expiry` and SSL health information. Engine exposes
`yuheng.tls.certificate.expiry.epoch.seconds`; after an atomic certificate replacement, the
protected `POST /actuator/gatewayTls` endpoint can perform bounded drain and rebuild the HTTP/RPC
listeners. This Compose mTLS example explicitly disables that endpoint. Management exposes only health/info/metrics on the container network and loopback host mappings so the data-plane proxy can remove unready replicas. The proxy never forwards management ports. Re-enable reload only through a separately controlled in-container management channel.

## OpenTelemetry

The Engine records Request, Provider Attempt, Tianshu Apply, and Kafka Send spans through Micrometer
Observation and the OTel Bridge. No Collector is configured by default. Enable OTLP explicitly:

```text
MANAGEMENT_OTLP_TRACING_EXPORT_ENABLED=true
MANAGEMENT_OTLP_TRACING_ENDPOINT=https://otel-collector.example/v1/traces
MANAGEMENT_TRACING_SAMPLING_PROBABILITY=0.1
```

A valid upstream W3C `traceparent` sampling flag takes precedence; the local sampling probability
is used only when the caller provides no W3C Parent. High-cardinality fields such as Operation,
Route, Provider Instance, and Event ID stay in spans and are not added to low-cardinality metric
tags. Collector unavailability does not affect Yuheng business responses.

## Known deployment boundaries

- PostgreSQL, Redis, Kafka, and Admin in the base `compose.yml` remain single-node development dependencies;
- `compose.ha.yml` validates stateless dual Admin only and does not claim that single-node PostgreSQL/Redis/Kafka are HA;
- Providers are discovered only through the Tianshu Registry, and rules are distributed only through Tianshu DB/Redis/PubSub;
- Tianshu bootstrap is direct unary gRPC through a configured logical target; there is no machine HTTP fallback, Tianshu self-registration, streaming configuration channel, or sticky-session requirement;
- Nacos, Dubbo, and Nginx management are outside this deployment;
- The dedicated data-plane HAProxy selects fixed API_RPC/MCP backend pools; external DNS, certificates and production ingress remain operator-owned;
- Secret Manager, NetworkPolicy, and external observability xingyuan remain owned by the deployment xingyuan.

## Dual-role state, credentials, and cutover

- Both roles share Tianshu biz/env/appCode/namespace (`ge`) but each replica has a unique Config Client/Registry/Node identity. The role is fixed by the executable, never a mode flag.
- `tianshu-rpc-credentials.yml` retains all three existing credential capabilities and adds separate MCP Runtime/Registry credentials. Spring list overrides replace the whole list, so configuring only indices3/4 is invalid. The example retains the existing wildcard scopes; constrain production scopes against actual Provider access.
- Provision separate Tianquan-Shoubing Resource IDs/URIs for the API and MCP processes. Business MCP Server resources, tokens, audiences and permission contracts stay unchanged. Replace example placeholders with registered identities; this directory does not deploy Tianquan-Shoubing.
- MCP sessions/subscriptions share Redis; tasks/approvals use existing gateway_admin tables, with Flyway owned by Admin. API has no MCP datasource. Provision the existing shared `YUHENG_MCP_ARTIFACT_DIRECTORY` with UID/GID10001 access before startup; scripts do not chown or erase user artifacts.
- Four independent LKG volumes prevent cross-process writes. Start Tianshu/Admin/Providers, then Engines; after a valid release run `./scripts/wait-ready.sh --engines`. Require both roles and every online replica to ACK the same Release/Version/Checksum.
- The proxy preserves18081 and routes `/mcp/`, `/legacy/mcp/`, and `/.well-known/oauth-protected-resource/mcp/` to MCP. Other paths use API_RPC, retaining Host/authentication/protocol headers. INTERNAL HTTP/gRPC retain separate listeners.
- `haproxy.data-plane.mtls.cfg` terminates external TLS and verifies each TLS backend certificate/hostname. Control-plane `haproxy.cfg` remains TCP passthrough. Supply a proxy PEM with server/client usage and stable external SAN; each Engine certificate covers its service DNS name. No verify-none option is used.
- A failed role retains its previous snapshot and Admin reports inconsistency. Do not publish a new Release while old Combined and split binaries coexist. Start MCP dark, validate, cut MCP routing, replace API_RPC, then retire old nodes. Roll back only the affected route/artifact; retain databases and LKG.
- `run-mcp-conformance.sh` defaults to MCP `/mcp/commerce`; publish that Server and satisfy its authentication first. Explicit official-SDK fixture URLs remain supported but are not Engine acceptance evidence. The security script includes MCP Context and unified-artifact compatibility.
- Static rendering/unit tests do not prove live Tianquan-Shoubing integration, TLS handshakes, browser flows or HA. Complete those runtime gates before production rollout.

Health-check and TLS syntax follows the [HAProxy3.1 configuration reference](https://docs.haproxy.org/3.1/configuration.html).
