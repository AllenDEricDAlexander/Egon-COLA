# Egon COLA Gateway Platform

[English](README.md) | [中文](README.zh-CN.md)

The Gateway platform is Egon COLA's self-built HTTP and RPC infrastructure system. It
contains a Reactor Netty data plane, a Spring Boot management control plane, a
provider-facing reporting starter, and an HTTP Provider lease runtime. The Admin
publishes immutable rule releases through DDC; the Engine discovers providers,
selects healthy instances, and forwards HTTP and unary RPC traffic.

## Architecture

```text
Admin Web ── authenticated API ──> Gateway Admin ── publish ──> DDC
                                                           │
HTTP/RPC client ──> Gateway Engine <── rule release ────────┘
                         │
                         ├── route, security, traffic, and observability pipeline
                         ├── DDC lease-based Provider / Gateway registry
                         └── HTTP or unary RPC upstream ──> Provider
```

The data plane keeps the active release immutable and swaps it atomically. Provider
definitions, leases, health, and interface reports are reconciled through DDC. The
Engine can retain valid in-memory state and its last-known-good release during a
temporary DDC outage, but a cold-start Engine must not claim Ready without the
required rule and provider state.

## Modules

| Module | Responsibility | Business dependency entry |
|---|---|---|
| `egon-cola-platform-gateway-contract` | Stable cross-process contracts for rules, providers, releases, and events | No |
| `egon-cola-platform-gateway-core` | Framework-free data-plane models, filters, routing, security, and SPI | No |
| `egon-cola-platform-gateway-engine` | Executable HTTP/RPC data plane, listeners, upstream clients, health, and telemetry | No |
| `egon-cola-platform-gateway-admin` | Executable management control plane, persistence, release compilation, authentication, and OpenAPI | No |
| `egon-cola-platform-gateway-starter` | Provider interface-definition reporting and downstream integration | Yes |
| `egon-cola-platform-gateway-provider-runtime` | HTTP Provider DDC registration and lease lifecycle | Yes |
| `egon-cola-platform-gateway-test` | Real HTTP/RPC providers, consumers, and live topology verification | No |

The Admin Web is a private React application colocated at
`egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web`; it is not a
Maven child. See its [frontend README](egon-cola-platform-gateway-admin-web/README.md).

## Runtime Capabilities

- Public and internal HTTP listeners with bounded request bodies, CORS, security
  filters, protocol retries, idempotency propagation, and graceful drain.
- Transparent OpenAI-compatible HTTP transport for raw JSON, SSE with per-buffer
  flush, streaming multipart uploads, and binary/multimodal request and response
  bodies, plus two-phase `ws`/`wss` Realtime WebSocket proxying.
- HTTP-to-HTTP, HTTP-to-RPC, and RPC-to-RPC forwarding through immutable route
  and provider snapshots.
- DDC lease-based provider discovery, active health probing, bounded provider
  attempts, load balancing, and removal of expired or unhealthy instances.
- Gateway Admin drafts, interface catalogs, release compilation, canonical hashes,
  authenticated management APIs, and runtime definition reconciliation.
- TLS/mTLS for HTTP, RPC, DDC, and management transports, plus controlled certificate
  reload and listener drain operations.
- Micrometer Observation / OpenTelemetry spans and bounded Kafka call-event
  projection. Telemetry failures must not change the business response.

## Trace Propagation

The Gateway data plane uses the W3C Trace Context support from
`egon-cola-component-common-trace`. Inbound requests build context only from
valid `traceparent`, `tracestate`, and `x-egon-request-id`. Gateway no longer
reads or writes `X-Trace-Id`, `x-trace-id`, or `x-egon-trace-id`. HTTP and RPC
upstreams create a distinct child span for each provider attempt; retries do
not reuse the same attempt `spanId`, while the whole request keeps one
`traceId`.

Gateway is already wired to Micrometer Observation / OpenTelemetry. When a
valid Observation span exists, `GatewayCallEventV1.Trace`, normal logs, and
downstream `traceparent` use that span. Without a tracer, Gateway falls back to
the lightweight `common-trace` generator.

## Consumption and Build

The Components BOM does not export Gateway artifacts. Business systems that need the
provider-facing integration surface may depend on `egon-cola-platform-gateway-starter`
or `egon-cola-platform-gateway-provider-runtime` with the repository release version.
Engine, Admin, Contract, Core, and test artifacts are internal platform modules and should
be built or deployed through the repository's Gateway topology.

The Java package namespace remains `top.egon.cola.component.gateway` in this migration to
preserve source and binary compatibility. The Maven coordinates and reactor ownership are
the authoritative platform boundary.

Run focused JVM verification:

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test \
  -am test
```

The live profile starts the real provider/consumer topology through the test harness.
It uses Testcontainers by default, or isolated host-local processes when `initdb`,
`postgres`, and `redis-server` are available on `PATH`:

```bash
./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite \
  -am -Pgateway-live verify

./mvnw -B -ntp \
  -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite \
  -am -Pgateway-live -Dgateway.live.infrastructure=local verify
```

## Operational Documentation

| Document | Purpose |
|---|---|
| [Gateway + DDC + RPC integration](docs/developer-integration.md) | End-to-end demo commands, success criteria, fault drills, and evidence boundaries |
| [Local deployment](deployment/README.md) | Compose build, ports, readiness, HA sample, TLS/mTLS, and startup/shutdown order |
| [Performance and fault drills](performance/README.md) | k6 smoke/baseline, long soak, resource sampling, and fixed fault scenarios |
| [Admin Web](egon-cola-platform-gateway-admin-web/README.md) | React build, tests, browser authentication, and API origin settings |

## Boundaries

- Nginx node management, dynamic Nginx configuration, and the external load balancer
  are outside the Gateway platform. The deployment environment owns ingress and L4/L7
  balancing in front of multiple Engine instances.
- The base Compose topology is a local development dependency set. The HA overlays
  validate multiple stateless Admin processes and proxy routing; they do not turn a
  single PostgreSQL, Redis, or Kafka node into a production HA service.
- The Gateway does not include a general account system or external IAM. Admin Web
  receives a verified IAM Bearer Token and Gateway Admin enforces the authenticated
  actor and capability boundary.
- The OpenAI route profile is a transport preset, not an AI platform. Gateway does
  not count tokens, charge usage, manage prompts or conversations, perform RAG or
  Agent orchestration, execute Function Calling, or select a business model. It
  recognizes routes, carries protocols, and transparently forwards bytes.
- Streaming component tests prove the in-process Gateway boundaries only. They do
  not prove public OpenAI connectivity, external/private-CA TLS, multi-process
  infrastructure, or flush/cache behavior of an outer Nginx or Ingress.
- The implementation and deployment contracts continue to evolve; use the focused
  tests and the linked deployment documentation as the current release evidence.
