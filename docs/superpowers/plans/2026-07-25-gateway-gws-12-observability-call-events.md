# GWS-12 Trace, Observability, and Call Event Implementation Plan

**Goal:** Produce one safe, immutable completion fact for every Engine business
call, propagate a valid caller trace, emit low-cardinality metrics/logs, deliver
one bounded best-effort Kafka event, and expose an idempotent Admin query
projection.

**Architecture:** A shared trace codec selects W3C `traceparent`, then
`X-Trace-Id`, then a secure generated ID. HTTP and RPC adapters create a
`GatewayCallObservation`; an Observer-style completion hub publishes the one
terminal snapshot to structured logging, Micrometer, and the event dispatcher.
The dispatcher uses a count-and-byte-bounded queue plus a dedicated Kafka
worker. Admin consumes into a replaceable `GatewayObservabilityStore` port and
PostgreSQL projection.

**Patterns considered:** Observer fits the single completion fact with multiple
independent sinks. Builder fits the staged lifecycle and provider attempts.
Adapter isolates Kafka and Admin storage. Transactional Outbox is rejected
because the request path owns no database transaction and the approved
reliability is explicitly best-effort non-blocking.

---

## Task 1: Trace contract and propagation

Add the W3C trace parser/context to `gateway-contract`, accepting only non-zero
32-hex IDs. Add HTTP and RPC selection/child-span helpers, response propagation,
and Admin management Trace filter. Test priority, normalization, conflict,
invalid generation, and child spans.

## Task 2: Completion observation

Add immutable lifecycle and attempt models plus an atomic one-shot observation
builder. Integrate HTTP and RPC terminal paths including no route, external
access rejection, security rejection, provider success/failure, timeout,
cancel, and internal error. Ensure event generation cannot alter the already
decided response.

## Task 3: Logs and metrics

Add a completion listener interface, structured access logger, and Micrometer
listener using bounded label dimensions. Never include bodies, headers, query,
credentials, principal identity, provider payload, or exception stacks in event
fields.

## Task 4: Event contract and bounded dispatcher

Add `GatewayCallEventV1` to the contract module and a canonical JSON serializer
with a 64 KiB hard limit. Implement a queue bounded by count and estimated
bytes, drop-new semantics, independent worker, short shutdown drain, health
snapshot, and reasoned counters.

## Task 5: Kafka adapter

Add configurable Kafka producer defaults (`acks=all`, idempotence, bounded
delivery timeout, LZ4), partition key rules, content/schema/event headers, and
asynchronous callback accounting. Disabled production mode reports a warning;
Kafka failures never affect business calls.

## Task 6: Admin consumer projection

Add one new Flyway migration with event summaries, minute aggregates, and
consume failures. Implement an idempotent store port, poison-message isolation
callback, offset-after-transaction consumer handler, retention cleanup,
dashboard/trace/audit read APIs, and clear unavailable versus no-data state.

## Task 7: Verification

Test trace parsing, one-shot completion, retries in one top-level event, payload
redaction, queue rejection, shutdown drain, Kafka record shape, consumer
idempotency, minute aggregation, and frontend DTO compatibility. Run targeted
Engine/Admin reactors and `git diff --check`.

