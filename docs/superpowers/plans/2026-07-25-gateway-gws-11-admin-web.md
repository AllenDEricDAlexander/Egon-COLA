# GWS-11 Gateway Admin Web Implementation Plan

**Goal:** Deliver the independent React administration console defined by
GWS-11, connected only to Gateway Admin APIs and preserving trace,
revision, idempotency, release evidence, and access-zone constraints.

**Architecture:** A Vite/React/TypeScript feature-oriented application uses one
typed `fetch` client, TanStack Query for server state, React Router for resource
identity, Ant Design for management UI, and Ant Design Charts for bounded
aggregates. Features never call DDC, Redis, Kafka, or Engine directly. Draft and
release mutations remain server-authoritative.

**Patterns considered:** Facade is selected for the typed API client so trace,
contract, error, and actor behavior have one boundary. Strategy is used only for
route-specific form validation (HTTP versus RPC); a global state store and
generic repository layer are rejected because Query cache and feature-local
forms already own those responsibilities.

---

## Task 1: Bootstrap and quality gates

Create the Vite React TypeScript project, pin Node 22, configure strict
TypeScript, ESLint, Vitest, Testing Library, and independent npm scripts.
Install React Router, TanStack Query, Ant Design, Ant Design Icons, and Ant
Design Charts. Keep the module outside the Maven reactor.

Verification:

```bash
npm test -- --run
npm run build
```

## Task 2: Typed API and trace boundary

Implement API DTOs for Gateway Group, Catalog, Draft, Release, Projection,
Credential, Audit, and Observability resources. Implement:

- 32-hex trace and 16-hex span generation with Web Crypto;
- one logical trace across a mutation and its status polling;
- contract and actor headers;
- AbortSignal, JSON decoding, 204 handling, and typed errors;
- distinct 401, 403, 404, 409, 422, 5xx and network classifications;
- idempotency-key generation for writes;
- no browser persistence of HMAC/DDC secrets.

Add unit tests for trace headers, error mapping, retry trace reuse, and contract
version mismatch.

## Task 3: Application shell

Implement the responsive management layout, scoped Env/Namespace selector,
navigation, backend-status indicator, capability provider, error boundary,
query provider, and lazy feature routes. Warn before discarding dirty draft
forms when changing scope or leaving the page.

## Task 4: Dashboard and Gateway Groups

Implement overview cards, bounded request/error/latency charts with accessible
text summaries, Gateway Group list, group overview, Engine node table, and
runtime consistency panel. Always show instance ID, lease ID, observed time,
stale state, capabilities, active release, and last ACK separately.

## Task 5: Interface Catalog

Implement the application and three-level domain tree with search, immutable
STARTER badges, MANUAL edit affordances, operation detail, schemas, provider
identity, external accessibility, definition history, and route references.
Large schema panels load only when expanded.

## Task 6: Draft routes and policies

Implement HTTP/RPC route editors and policy sections. Enforce in the UI:

- PUBLIC cannot select `externalAccessible=false`;
- no static Provider URL input exists;
- current revision and idempotency key accompany saves;
- 409 retains local input and presents the server revision;
- policy unit and unsafe-combination warnings are explicit.

Backend validation remains definitive.

## Task 7: Release workbench

Implement validate, diff, publish, detail, retry, and rollback flows. Poll only
visible non-terminal releases. Render attempts and immutable Target evidence;
FAILED, TIMEOUT, UNKNOWN, and partial-applied states must never appear as
success. Retry retains release and target identity, while rollback visibly
creates a new release.

## Task 8: Provider, observability, and audit

Implement management-projection Provider views, bounded trace filters/details,
aggregate call charts, and audit filters/details. Sensitive bodies,
credentials, cookies, and raw headers are absent from DTO rendering.

## Task 9: Test and package

Add fixtures and component tests covering PUBLIC constraints, revision
conflicts, release state semantics, target ACK evidence, catalog source
semantics, capability behavior, sensitive-field exclusion, and polling cleanup.
Build the production bundle and run `git diff --check`.

