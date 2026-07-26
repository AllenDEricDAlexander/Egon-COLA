# Gateway Local Deployment and Runtime Boundaries

[中文](README.zh-CN.md) | [Gateway overview](../README.md)

This directory provides deployment examples for Gateway Engine, Gateway Admin, Admin Web,
and their local dependencies. It is not a production HA solution and does not manage Nginx
node load balancing or dynamic configuration.

## Build prerequisites

Build the three executable artifacts from the repository root:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin,egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-admin,egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-engine \
  -am clean package -DskipTests
```

Copy `.env.example` to `.env` and fill it locally with random credentials and a Base64-encoded
32-byte master key. Do not commit `.env`. The operator may then run:

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
| DDC Admin | 18070 | DDC OpenAPI/management |
| Gateway Admin | 18080 | Management API and health endpoints |
| Engine 1 PUBLIC | 18081 | External HTTP data plane |
| Engine 1 INTERNAL | 18082 | Internal HTTP data plane |
| Engine 1 Management | 18083 | Actuator |
| Engine 1 RPC Slot | 19090 | Egon RPC internal Gateway |
| Engine 2 PUBLIC | 18181 | Second external HTTP data plane |
| Engine 2 INTERNAL | 18182 | Second internal HTTP data plane |
| Engine 2 Management | 18183 | Second Actuator |
| Engine 2 RPC Slot | 19190 | Second Egon RPC internal Gateway |
| Admin Web | 18090 | React management page |

Persist each Engine's LKG directory independently. DDC Redis and the distributed rate-limit
Redis must use separate instances and data volumes. Initialize two PostgreSQL databases so
that DDC and Gateway Admin Flyway histories cannot interfere with each other.

## Health and release order

Recommended checks:

```text
DDC Admin  GET /api/v1/ddc/manifest
Admin      GET /actuator/health/liveness
Admin      GET /actuator/health/readiness
Engine     GET :18083/actuator/health/liveness
Engine     GET :18083/actuator/health/readiness
Admin Web  GET /healthz
```

An Engine process being alive does not mean that it is business-ready. For a first deployment,
start DDC/Admin first, wait for the Engine to register as a Config Client, and then have Admin
publish the first valid Rule Release. An Engine should receive traffic only after its listener,
valid rules, and required Providers are ready.

## Automated acceptance

The fast gate does not start external processes; it covers Java unit/component tests, Admin Web
type checking, Vitest, ESLint, and the production build. The real-topology gate uses
Testcontainers for PostgreSQL, two Redis instances, and Kafka, then starts real DDC, Admin, two
Engines, an HTTP Provider, an RPC Provider, and an RPC Consumer through the process harness:

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am -Pgateway-live verify
```

Docker must be available locally. The test verifies interface-definition reporting, rule
publication, registration and readiness of both Engines, HTTP/RPC forwarding, load balancing
across two Providers, Provider removal, rate limiting, and Kafka Trace projection. Logs and
redacted process parameters are written to `target/gateway-process-it`.

Release and shutdown order:

```text
Start: PostgreSQL/Redis/Kafka → DDC → Admin → Provider → Engine → Admin Web
Stop:  remove Engine traffic → bounded Engine drain → Provider → Admin → DDC → infrastructure
```

Compose reserves 30 seconds for graceful termination. A Kafka failure must not change the
business response, but dropped/failed events must be visible through metrics. When DDC is
temporarily unavailable, a running Engine may continue using valid in-memory state and LKG;
a cold-start node must not claim Ready on that basis.

## Control-plane HA

`compose.ha.yml` adds a second DDC Admin, a second Gateway Admin, and an HAProxy that only
forwards TCP on top of shared PostgreSQL, DDC Redis, and Kafka. It does not introduce Raft or
change the business Gateway boundary. DDC publish consistency is still provided by PostgreSQL
row locks, conditional version updates, and persistent publish tasks; Redis provides cache,
Registry, and notification functions. DDC Admin can connect to production Redis with:

```text
EGON_COLA_COMPONENT_DDC_ADMIN_REDIS_MODE=SENTINEL|CLUSTER
EGON_COLA_COMPONENT_DDC_ADMIN_REDIS_NODES[0]=redis://redis-1:26379
EGON_COLA_COMPONENT_DDC_ADMIN_REDIS_MASTER_NAME=ddc-master
```

The HA example is started by the operator:

```bash
docker compose --env-file .env \
  -f compose.yml -f compose.ha.yml --profile ha config
docker compose --env-file .env \
  -f compose.yml -f compose.ha.yml --profile ha up -d
```

Proxy ports `18270` and `18280` point to the two DDC Admin instances and the two Gateway Admin
instances respectively. Removing either Admin container causes the TCP health check to remove
that node. The other instance continues reading the same publish task, so startup recovery in
the second instance does not report a false failure.

RPC Gateway Slots use the DDC `INTERNAL_GATEWAY` instance set in the same way. The Consumer keeps
unchanged channels, adds channels for new Engines, and performs bounded drain for Engines that
are going offline; it selects endpoints with round robin. Only `UNAVAILABLE` at the Gateway
connection stage is retried on another node within the total deadline. Provider-stage failures
are not invoked again.

## TLS and mTLS

Production mode does not accept implicit plaintext. PUBLIC HTTP may use one-way TLS; INTERNAL
HTTP, RPC Slots, DDC management, and Gateway Admin management can require mTLS. Certificates,
private keys, and trust chains are injected only through read-only file paths. There is no
skip-SAN/authority-validation or Trust-All switch. Local plaintext must explicitly set
`development-plaintext=true` or `transport-security.mode=DEVELOPMENT_PLAINTEXT`.

`compose.mtls.yml` demonstrates PEM-file injection. `${GATEWAY_TLS_DIRECTORY}` must contain at
least:

```text
ca.crt
ddc-admin.crt / ddc-admin.key
ddc-admin-2.crt / ddc-admin-2.key
gateway-admin.crt / gateway-admin.key
gateway-admin-2.crt / gateway-admin-2.key
gateway-admin-web.crt / gateway-admin-web.key
gateway-engine.crt / gateway-engine.key
gateway-engine-2.crt / gateway-engine-2.key
```

Private keys must be unencrypted PKCS#8 PEM. Certificate SANs must cover actual connection
names. With HA and mTLS, DDC and Gateway Admin server certificates must also cover
`control-plane-proxy`. Validate the configuration with:

```bash
docker compose --env-file .env \
  -f compose.yml -f compose.mtls.yml config
docker compose --env-file .env \
  -f compose.yml -f compose.ha.yml -f compose.mtls.yml \
  -f compose.ha-mtls.yml --profile ha config
```

Spring SSL Bundles use `reload-on-update=true`, so DDC Admin and Gateway Admin watch for PEM
file updates. Actuator exposes `ssl.chain.expiry` and SSL health information. Engine exposes
`gateway.tls.certificate.expiry.epoch.seconds`; after an atomic certificate replacement, the
protected `POST /actuator/gatewayTls` endpoint can perform bounded drain and rebuild the HTTP/RPC
listeners. The endpoint is not created or exposed by default: the `operations` Profile must
explicitly enable it and bind the Spring Management Server to `127.0.0.1` inside the container.
`compose.mtls.yml` enables this Profile; the deployment platform must call it through a
controlled in-container channel and must not forward it externally.

## OpenTelemetry

The Engine records Request, Provider Attempt, DDC Apply, and Kafka Send spans through Micrometer
Observation and the OTel Bridge. No Collector is configured by default. Enable OTLP explicitly:

```text
MANAGEMENT_OTLP_TRACING_EXPORT_ENABLED=true
MANAGEMENT_OTLP_TRACING_ENDPOINT=https://otel-collector.example/v1/traces
MANAGEMENT_TRACING_SAMPLING_PROBABILITY=0.1
```

A valid upstream W3C `traceparent` sampling flag takes precedence; the local sampling probability
is used only when the caller provides no W3C Parent. High-cardinality fields such as Operation,
Route, Provider Instance, and Event ID stay in spans and are not added to low-cardinality metric
tags. Collector unavailability does not affect Gateway business responses.

## Known deployment boundaries

- PostgreSQL, Redis, Kafka, and Admin in the base `compose.yml` remain single-node development dependencies;
- `compose.ha.yml` validates stateless dual Admin only and does not claim that single-node PostgreSQL/Redis/Kafka are HA;
- Providers are discovered only through the DDC Registry, and rules are distributed only through DDC DB/Redis/PubSub;
- Nacos, Dubbo, and Nginx management are outside this deployment;
- Compose exposes two Engine ports, but ingress L4/L7 load balancing remains owned by the deployment platform;
- Secret Manager, NetworkPolicy, and external observability platforms remain owned by the deployment platform.
