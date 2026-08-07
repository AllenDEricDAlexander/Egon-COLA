# Gateway, DDC, and RPC Developer Integration Runbook

[中文](developer-integration.zh-CN.md) | [Gateway overview](../README.md)

This runbook exercises the local end-to-end path: Gateway Admin publishes rules through
DDC; two Engines subscribe to rules and Provider leases; MVC, WebFlux, and RPC Providers
register and report interfaces; and the RPC Consumer discovers internal Gateways only.
Nginx, production HA, external IAM, and production certificates are outside this demo.

## Prerequisites and evidence boundary

Install JDK 21, Docker Compose, `curl`, `jq`, and `openssl`. Then create local secrets:

```bash
cd egon-cola-platforms/egon-cola-platform-gateway/deployment
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

## OpenAI-compatible transport routes

An OpenAI-compatible route remains a normal Gateway route bound to an HTTP Operation and
a lease-discovered Provider. It does not contain a static upstream URL or model selection.
The following are canonical `content` objects for the draft route API; replace each
`operationId` in the surrounding route request with an ID returned by the interface
catalog.

General JSON, SSE auto-detection, multipart upload, and multimodal payloads can share an
`OPENAI_HTTP` streaming route:

```json
{
  "host": "ai.example.com",
  "httpMethod": "POST",
  "pathPattern": "/v1/**",
  "accessZones": ["PUBLIC"],
  "priority": 0,
  "transportPolicy": {
    "profile": "OPENAI_HTTP",
    "transportProtocol": "HTTP",
    "requestBodyMode": "STREAMING",
    "responseMode": "AUTO_STREAM",
    "maxRequestBodyBytes": 536870912,
    "connectTimeoutMs": 10000,
    "responseHeaderTimeoutMs": 120000,
    "streamIdleTimeoutMs": 90000,
    "totalTimeoutMs": 1800000,
    "bodyLogEnabled": false,
    "retryEnabled": false
  }
}
```

Use an explicit response mode when the route contract is narrower. Keep
`requestBodyMode=STREAMING` for multipart and large uploads; no multipart parser runs in
the Gateway.

| Route intent | `requestBodyMode` | `responseMode` |
|---|---|---|
| Ordinary JSON or mixed OpenAI endpoints | `STREAMING` | `AUTO_STREAM` |
| Responses/chat SSE endpoint | `STREAMING` | `SSE` |
| Multipart audio/file upload | `STREAMING` | `STANDARD` or `AUTO_STREAM` |
| Image/audio binary download | `STREAMING` | `BINARY_STREAM` |

Realtime WebSocket uses a separate GET route and the same catalog/provider boundary:

```json
{
  "host": "ai.example.com",
  "httpMethod": "GET",
  "pathPattern": "/v1/realtime",
  "accessZones": ["PUBLIC"],
  "priority": 0,
  "transportPolicy": {
    "profile": "OPENAI_HTTP",
    "transportProtocol": "WEBSOCKET",
    "websocketIdleTimeoutMs": 300000,
    "websocketMaxFrameBytes": 16777216,
    "bodyLogEnabled": false,
    "retryEnabled": false
  }
}
```

The Engine uses `ws` or `wss` from the selected Provider's secure transport metadata,
forwards Text, Binary, Continuation, Ping/Pong, and legal Close frames, and negotiates a
client-offered subprotocol. It disables WebSocket extensions by default. Upstream
handshake rejection remains an ordinary HTTP error because downstream `101` is not sent
until the upstream handshake succeeds.

`OPENAI_HTTP` defaults to a 512 MiB request limit, 10 s connect timeout, 120 s response
header timeout, 90 s stream idle timeout, 30 min total timeout, disabled Body logging,
and disabled retry. WebSocket defaults are 5 min idle and 16 MiB per frame. Explicit route
fields override these defaults but remain capped by the Engine safety limits; all Engine
nodes in one Gateway Group must use identical safety limits.

| Field | Meaning |
|---|---|
| `maxRequestBodyBytes` | Streaming byte ceiling; declared oversize is rejected before connect and chunked oversize stops mid-stream. |
| `connectTimeoutMs` | TCP/TLS connection establishment budget. |
| `responseHeaderTimeoutMs` | Budget to receive upstream response headers after request send begins. |
| `streamIdleTimeoutMs` | Maximum inactivity between request or response stream buffers. |
| `totalTimeoutMs` | End-to-end HTTP budget; active SSE data does not reset it. |
| `websocketIdleTimeoutMs` | Shared bidirectional frame-idle budget; Ping/Pong are activity. |
| `websocketMaxFrameBytes` | Per-frame limit; violation closes with code 1009. |
| `bodyLogEnabled` | Enables only the existing bounded body sample; the OpenAI profile disables it. |
| `retryEnabled` | Route override subject to safety gates; streaming/OpenAI POST/WebSocket and every committed exchange remain non-retryable. |

End-to-end headers such as `Content-Type`, `Authorization`, `OpenAI-Organization`,
`OpenAI-Project`, `Idempotency-Key`, `traceparent`, and `tracestate` are preserved when
allowed by the route security profile. Fixed and `Connection`-declared hop-by-hop headers
are removed. Bodies are passed as `DataBuffer` streams and are not parsed and
re-serialized. SSE removes `Content-Length`, sets `Cache-Control: no-cache, no-transform`
and `X-Accel-Buffering: no`, and flushes each emitted buffer. Binary bodies never pass
through String or JSON conversion.

HTTP-to-RPC and RPC routes retain the existing aggregated unary behavior. They cannot use
WebSocket, streaming request bodies, SSE, binary streaming, or `OPENAI_HTTP` defaults.
Retries are never allowed after upstream headers, downstream headers, an SSE body buffer,
a WebSocket `101`, or a forwarded frame for OpenAI transport. A downstream disconnect
cancels the active upstream body/session immediately.

### Compatibility and rollout order

New Engines continue to read old v1 releases with no `transportPolicy`; those routes use
the legacy aggregated HTTP/RPC behavior. An old Engine must not be given a release that
contains new transport fields. Roll out in this order:

1. Upgrade every Engine in the Gateway Group and wait until all nodes are Ready.
2. Verify homogeneous Engine transport safety limits and runtime consistency.
3. Upgrade Gateway Admin and Admin Web.
4. Only then create and publish routes containing `transportPolicy`.

Keep old rules active during a mixed-version window. Failed activation continues to rely
on the existing last-known-good release. A historical UI draft without `host` must be
repaired manually; the Admin does not synthesize a wildcard `*`.

This Gateway capability deliberately stops at transport. It does not count tokens, bill
usage, manage prompts or conversations, perform RAG or Agent orchestration, execute
Function Calling, or select a business model.

## Manual success criteria

```bash
curl -fsS http://127.0.0.1:18070/actuator/health/readiness
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
./mvnw -B -ntp -f pom.xml \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite \
  -am test
```

Opt-in real topology (starts containers and child JVMs):

```bash
./mvnw -B -ntp -f pom.xml \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite \
  -am -Pgateway-live verify
```

Host-only real topology (requires `initdb`, `postgres`, and `redis-server` on
`PATH`; starts isolated temporary infrastructure and child JVMs):

```bash
./mvnw -B -ntp -f pom.xml \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite \
  -am -Pgateway-live -Dgateway.live.infrastructure=local verify
```

The base demo and in-process streaming tests do not verify Redis Sentinel/Cluster,
PostgreSQL or Kafka HA, multi-Admin failover, production TLS/mTLS and rotation, public
OpenAI connectivity, private CAs, external load balancing, or Kubernetes. The Gateway
cannot force an outer Nginx/Ingress to flush or disable its cache. A renderable overlay or
component fixture is not runtime evidence; validate those paths in the target environment.
