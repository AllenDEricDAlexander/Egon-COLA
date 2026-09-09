# Egon COLA Yuheng Platform

[English](README.md) | [中文](README.zh-CN.md)

The Yuheng platform is Egon COLA's self-built HTTP and RPC infrastructure system. It
contains a Reactor Netty data plane, a Spring Boot management control plane, a
provider-facing reporting starter, and an HTTP Provider lease runtime. The Admin
publishes immutable rule releases through Tianshu; the Engine discovers providers,
selects healthy instances, and forwards HTTP and unary RPC traffic.

## Architecture

```text
Admin Web ── authenticated API ──> Yuheng Admin ── publish ──> Tianshu
                                                           │
HTTP/RPC client ──> Yuheng Engine <── rule release ────────┘
                         │
                         ├── route, security, traffic, and observability pipeline
                         ├── Tianshu lease-based Provider / Yuheng registry
                         └── HTTP or unary RPC upstream ──> Provider
```

The data plane keeps the active release immutable and swaps it atomically. Provider
definitions, leases, health, and interface reports are reconciled through Tianshu. The
Engine can retain valid in-memory state and its last-known-good release during a
temporary Tianshu outage, but a cold-start Engine must not claim Ready without the
required rule and provider state.

Tianshu is the bootstrap exception: Yuheng Admin, Engines, Providers, and Consumers use
a locally configured direct Tianshu gRPC target on port `19080` with `round_robin`; Tianshu
does not register or discover itself. Redis Pub/Sub still carries change notifications.
All ordinary RPC services remain Tianshu-supported and business calls still traverse the
discovered Yuheng set rather than connecting directly to Providers.

## Modules

| Module | Responsibility | Business dependency entry |
|---|---|---|
| `yuheng-contract` | Stable cross-process contracts for rules, providers, releases, and events | No |
| `yuheng-core` | Framework-free data-plane models, filters, routing, security, and SPI | No |
| `yuheng-mcp-core` | Shared MCP protocol, task, session, artifact, and subscription runtime primitives | No |
| `yuheng-biz-gateway` | Executable HTTP/RPC data plane, listeners, upstream clients, health, and telemetry | No |
| `yuheng-admin` | Executable management control plane, persistence, release compilation, authentication, and OpenAPI | No |
| `yuheng-starter` | Provider interface-definition reporting plus Yuheng metadata contribution to Tianshu HTTP registration | Yes |
| `yuheng-test` | Real HTTP/RPC providers, consumers, and live topology verification | No |

The Admin Web is a private React application colocated at
`egon-cola-yuheng/yuheng-admin-web`; it is not a
Maven child. See its [frontend README](yuheng-admin-web/README.md).

## Runtime Capabilities

- Public and internal HTTP listeners with bounded request bodies, CORS, security
  filters, protocol retries, idempotency propagation, and graceful drain.
- Transparent OpenAI-compatible HTTP transport for raw JSON, SSE with per-buffer
  flush, streaming multipart uploads, and binary/multimodal request and response
  bodies, plus two-phase `ws`/`wss` Realtime WebSocket proxying.
- HTTP-to-HTTP, HTTP-to-RPC, and RPC-to-RPC forwarding through immutable route
  and provider snapshots.
- Tianshu lease-based provider discovery, active health probing, bounded provider
  attempts, load balancing, and removal of expired or unhealthy instances.
- Yuheng Admin drafts, interface catalogs, release compilation, canonical hashes,
  authenticated management APIs, and runtime definition reconciliation.
- TLS/mTLS for HTTP, RPC, direct Tianshu RPC, and management transports, plus controlled certificate
  reload and listener drain operations.
- Micrometer Observation / OpenTelemetry spans and bounded Kafka call-event
  projection. Telemetry failures must not change the business response.

## MCP extension boundary

The current durable Tasks workflow is an Egon extension, not the MCP 2025-11-25
Tasks contract. Stable initialization advertises it under
`capabilities.experimental["top.egon/tasks"]`, and UI-resource support under
`capabilities.experimental["top.egon/apps"]`. Existing task policies and
`tasks/get`, `tasks/update`, and `tasks/cancel` remain available; standard task
augmentation negotiation, `tasks/list`, and `tasks/result` are not implemented.
RC discovery retains its existing dialect-specific description. A successful
resource or prompt smoke test is not proof of standard Tasks or Apps compatibility.

## OAuth Resource binding

Yuheng resolves the expected Resource Server from the trusted route target,
not from a caller-supplied header or request parameter. A route targets one
exact `bizCode + appCode + environment` triple, which maps to one absolute
Resource URI such as
`https://api.egon.internal/prod/permission/tianquan-shoubing`. The Tianquan-Shoubing adapter accepts only a
single-audience access token whose `aud` and `resource_version` match that route
Resource and whose principal is either `USER` or `SERVICE`.

Yuheng performs authentication, exact Resource binding, trusted identity-header
replacement, and routing. It does not decide user roles or interface/data/field
permissions and does not ask Tianquan-Jianshen whether a service may call another service.
The downstream service repeats exact Resource validation: USER continues into
Tianquan-Jianshen authorization, while SERVICE is checked locally against the operation's
required Tianquan-Shoubing scope.

Yuheng Admin and Engine are Resource Servers themselves and use owner-only
private-key files to obtain Admission Tickets for Tianshu registration. Resource
disable revokes only the matching route-provider leases and blocks new tokens
and tickets. After restoring the Resource, key, grants, and route definition,
instances obtain fresh tickets and reconcile normally. Deploy Tianquan-Shoubing V2 and Tianshu V8
before Yuheng V11; Yuheng V11 removes the legacy audience column and is not
rollback-compatible with binaries that still expect it.

## Trace Propagation

The Yuheng data plane uses the W3C Trace Context support from
`egon-cola-component-common-trace`. Inbound requests build context only from
valid `traceparent`, `tracestate`, and `x-egon-request-id`. Yuheng no longer
reads or writes `X-Trace-Id`, `x-trace-id`, or `x-egon-trace-id`. HTTP and RPC
upstreams create a distinct child span for each provider attempt; retries do
not reuse the same attempt `spanId`, while the whole request keeps one
`traceId`.

Yuheng is already wired to Micrometer Observation / OpenTelemetry. When a
valid Observation span exists, `GatewayCallEventV1.Trace`, normal logs, and
downstream `traceparent` use that span. Without a tracer, Yuheng falls back to
the lightweight `common-trace` generator.

## Consumption and Build

The Components BOM does not export Yuheng artifacts. Business systems that publish
Yuheng definitions depend only on `yuheng-starter`; it composes
the Tianshu HTTP registration starter. Applications that need HTTP registration without
Yuheng definition reporting may depend directly on
`egon-cola-tianshu-http-registration-starter` with the repository release version.
Engine, Admin, Contract, Core, and test artifacts are internal xingyuan modules and should
be built or deployed through the repository's Yuheng topology.

HTTP registration Java types now live under
`top.egon.cola.component.tianshu.http.registration`; Yuheng types remain under
`top.egon.cola.component.yuheng`.

Run focused JVM verification:

```bash
./mvnw -B -ntp \
  -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-test \
  -am test
```

The live profile starts the real provider/consumer topology through the test harness.
It uses Testcontainers by default, or isolated host-local processes when `initdb`,
`postgres`, and `redis-server` are available on `PATH`:

```bash
./mvnw -B -ntp \
  -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite \
  -am -Pyuheng-live verify

./mvnw -B -ntp \
  -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite \
  -am -Pyuheng-live -Dgateway.live.infrastructure=local verify
```

## Operational Documentation

| Document | Purpose |
|---|---|
| [Yuheng + Tianshu + RPC integration](docs/developer-integration.md) | End-to-end demo commands, success criteria, fault drills, and evidence boundaries |
| [Local deployment](deployment/README.md) | Compose build, ports, readiness, HA sample, TLS/mTLS, and startup/shutdown order |
| [Performance and fault drills](performance/README.md) | k6 smoke/baseline, long soak, resource sampling, and fixed fault scenarios |
| [Admin Web](yuheng-admin-web/README.md) | React build, tests, browser authentication, and API origin settings |

## Boundaries

- Nginx node management, dynamic Nginx configuration, and the external load balancer
  are outside the Yuheng platform. The deployment environment owns ingress and L4/L7
  balancing in front of multiple Engine instances.
- The base Compose topology is a local development dependency set. The HA overlays
  validate multiple stateless Admin processes and proxy routing; they do not turn a
  single PostgreSQL, Redis, or Kafka node into a production HA service.
- The Yuheng does not include a general account system or external IAM. Admin Web
  receives a verified IAM Bearer Token and Yuheng Admin enforces the authenticated
  actor and capability boundary.
- The OpenAI route profile is a transport preset, not an AI xingyuan. Yuheng does
  not count tokens, charge usage, manage prompts or conversations, perform RAG or
  Agent orchestration, execute Function Calling, or select a business model. It
  recognizes routes, carries protocols, and transparently forwards bytes.
- Streaming component tests prove the in-process Yuheng boundaries only. They do
  not prove public OpenAI connectivity, external/private-CA TLS, multi-process
  infrastructure, or flush/cache behavior of an outer Nginx or Ingress.
- The implementation and deployment contracts continue to evolve; use the focused
  tests and the linked deployment documentation as the current release evidence.
