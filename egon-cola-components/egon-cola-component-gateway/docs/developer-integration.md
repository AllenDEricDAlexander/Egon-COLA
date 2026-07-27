# Gateway, DDC, and RPC Developer Integration Runbook

[中文](developer-integration.zh-CN.md) | [Gateway overview](../README.md)

This runbook exercises the local end-to-end path: Gateway Admin publishes rules through
DDC; two Engines subscribe to rules and Provider leases; MVC, WebFlux, and RPC Providers
register and report interfaces; and the RPC Consumer discovers internal Gateways only.
Nginx, production HA, external IAM, and production certificates are outside this demo.

## Prerequisites and evidence boundary

Install JDK 21, Docker Compose, `curl`, `jq`, and `openssl`. Then create local secrets:

```bash
cd egon-cola-components/egon-cola-component-gateway/deployment
cp .env.example .env
chmod 600 .env
./scripts/demo.sh doctor
```

Default tests do not start Docker. Shell syntax, `demo.sh --help`, fake-Docker safety tests,
and `docker compose config --quiet` are static evidence only. The PostgreSQL, two-Redis,
Kafka, dual-Engine, and real-Provider topology is proven only after an operator runs the
complete lifecycle below.

## Topology and ports

```text
Admin Web :18090 -> Gateway Admin :18080 -> DDC Admin :18070
Engine 1 PUBLIC/INTERNAL :18081/:18082, RPC :19090
Engine 2 PUBLIC/INTERNAL :18181/:18182, RPC :19190
MVC :18084, WebFlux :18085, RPC Provider :18086/:19091
RPC Consumer :18087 -> DDC Gateway set -> RPC Provider
```

DDC Redis and distributed-rate Redis are separate services and volumes. Each Engine has
its own persistent LKG volume.

## Command lifecycle

Run in this order:

```bash
./scripts/demo.sh build
./scripts/demo.sh up-control
./scripts/demo.sh init
./scripts/demo.sh up-providers
./scripts/demo.sh publish
./scripts/demo.sh up-consumer
./scripts/demo.sh verify
./scripts/demo.sh logs
./scripts/demo.sh down
```

`init` generates a 12-hour local JWT and creates the HTTP/RPC applications, reporting
credentials, and Gateway Group. Secrets and IDs stay in ignored, mode-0600 files under
`.demo/`. `publish` resolves operation IDs from reported `methodIdentity` values, uploads
HTTP and HTTP-to-RPC routes plus a distributed-rate policy, and checks the release.

`down` preserves volumes. Use `./scripts/demo.sh purge` only to irreversibly delete this
marked local demo's PostgreSQL, Redis, Kafka, and Engine LKG volumes. Purge requires the
local marker and an `egon-cola-gateway-demo-*` project name.

## Manual success criteria

```bash
curl -fsS http://127.0.0.1:18070/api/v1/ddc/manifest
curl -fsS http://127.0.0.1:18080/actuator/health/readiness
curl -fsS -H 'Host: providers.gateway.demo' \
  http://127.0.0.1:18081/api/providers/manual-1 | jq
curl -fsS 'http://127.0.0.1:18087/test/rpc/echo?message=manual-rpc' | jq
```

Repeated Provider calls should observe both `mvc` and `webflux`. RPC responses should
contain the message, trace ID, and `rpc-provider-demo`. Query runtime consistency with the
JWT and group ID under `.demo/`; success requires `consistent=true` and
`readyEngineNodeCount=2`. Provider and trace projections must identify instances,
protocol, provider service, and Engine instance.

## Fault drills and diagnosis

- Gracefully stop MVC: WebFlux continues; restart produces a new lease ID.
- Force-kill WebFlux: it disappears after TTL; MVC continues.
- Stop one Engine: the RPC Consumer moves to the remaining Gateway Slot; a restarted
  Engine obtains a new lease and returns to rotation.
- Pause DDC: a ready Engine may use valid memory/LKG, but a cold Engine must not claim Ready.
- Pause Kafka: business responses remain unchanged while failure/drop metrics increase.

Collect `.demo/logs/compose.log` and inspect readiness, runtime consistency, Provider/Engine
projections, and traces before retrying. Do not replace state polling with fixed sleeps.

## Automated and unverified boundaries

Default tests:

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am test
```

Opt-in real topology (starts containers and child JVMs):

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am -Pgateway-live verify
```

Host-only real topology (requires `initdb`, `postgres`, and `redis-server` on
`PATH`; starts isolated temporary infrastructure and child JVMs):

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am -Pgateway-live -Dgateway.live.infrastructure=local verify
```

The base demo does not verify Redis Sentinel/Cluster, PostgreSQL or Kafka HA, multi-Admin
failover, production TLS/mTLS and rotation, external load balancing, or Kubernetes. A
renderable overlay is not runtime evidence; validate those paths in the target environment.
