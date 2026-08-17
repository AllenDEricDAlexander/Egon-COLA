# Per-interface Contract Design

Read this reference whenever Chapter 9 contains an HTTP, RPC, event/message, CLI, scheduled-job, or internal-service contract. An inventory is only the index; every inventory item must have a complete detailed subsection.

## Contents

- [Contract inventory](#contract-inventory)
- [Required per-interface subsection](#required-per-interface-subsection)
- [Contract drafting sequence](#contract-drafting-sequence)
- [Complete HTTP worked example](#complete-http-worked-example)
- [List-query expansion example](#list-query-expansion-example)
- [Non-HTTP contracts](#non-http-contracts)
- [Depth and consistency gate](#depth-and-consistency-gate)
- [Contract review failures](#contract-review-failures)

## Contract inventory

Assign stable IDs such as `API-001`, `RPC-001`, `EVENT-001`, `JOB-001`, or `INTERNAL-001`.

One ID represents one atomic protocol operation: one HTTP Method + URL, one RPC service method, one event/topic schema contract, one job, or one internal method. Do not group a CRUD family, several URLs, or collection/detail/status operations into one row; shared rules may be referenced after each operation remains independently specified.

| ID | Name/purpose | Kind | Consumer | Owner | Method + URL / symbol / topic | Input | Output | Auth/tenant | Error model | Idempotency/version | Requirements |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |

The inventory and detailed subsections must be bijective: no inventory item may lack details, and no detailed contract may be absent from the inventory.

## Required per-interface subsection

Use one subsection per contract ID and keep the following structure.

### 1. Identity and purpose

- exact name, owner, consumer/frontend page, and business purpose;
- protocol plus HTTP method and absolute application route, or exact RPC service/method, topic, job, CLI, or internal symbol;
- request/response content type, version, timeout, retry, and compatibility policy;
- authentication, permission, tenant source, sensitive-data handling, rate limit, and audit rule;
- idempotency key/source, duplicate behavior, ordering, and concurrency semantics.

Do not invent a URL from a controller name. Verify class-level and method-level mappings, context path, gateway prefix, and version prefix from repository evidence.

### 2. Request contract

Document path, query, header, cookie, multipart, and body inputs separately. For every parameter include:

| Name | Location | Type/format | Required/null | Default | Validation/range/enum | Meaning | Example | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |

Validation must state exact rules: length, numeric range/precision, regex, enum values, time zone, collection size, uniqueness, cross-field conditions, trimming/case handling, and behavior for missing, `null`, empty, blank, unknown, or duplicated values as applicable.

When a request body exists, show the full nested payload as documentation-only `jsonc`, followed by a field table when type/nullability/source needs more precision:

```jsonc
{
  "name": "example", // Required. User-visible name; 1-64 trimmed characters.
  "enabled": true, // Optional. Whether the record is active; defaults to true.
  "items": [ // Required. Unique items; 1-100 entries.
    {
      "itemId": 1001, // Required. Existing item identifier visible to this tenant.
      "quantity": 2 // Required. Requested quantity; integer from 1 to 9999.
    }
  ]
}
```

`jsonc` comments are documentation only; the real wire payload remains strict JSON. Every field key, including wrapper, nested object, array, paging, and metadata fields, requires a line-end comment.

### 3. Success response

State the HTTP status or protocol outcome, response headers, actual repository response wrapper, and full response body. For HTTP JSON responses, use `jsonc` and add a line-end meaning comment to every field:

```jsonc
{
  "code": "SUCCESS", // Stable application result code defined by the current response wrapper.
  "message": "ok", // User/developer-readable result message; not a localization key unless the repository defines it so.
  "data": { // Successful payload; nullability must match the repository wrapper contract.
    "id": 1001, // Stable resource identifier returned to the frontend.
    "status": "ACTIVE", // Current state; list all allowed values and frontend meaning.
    "createdAt": "2026-08-17T10:15:30+08:00" // Creation time in the repository-standard format and time zone.
  }
}
```

Do not use `...`, omit inherited wrapper fields, or show a DTO class name as if it were a JSON response. Expand nested objects and one representative array item. State types, required/null/default semantics, enum values, precision, time format/time zone, sensitive-field masking, derivation/source, and ordering.

### 4. Error response and status mapping

Define transport status, stable business error code, message semantics, retryability, frontend behavior, and the condition that produces each error.

| Condition | HTTP/protocol status | Business code | Response shape | Retryable | Frontend handling |
| --- | --- | --- | --- | --- | --- |

Show the actual error JSON wrapper as `jsonc` when the protocol uses JSON. Separate validation, authentication, permission, not-found, conflict/idempotency, rate-limit, dependency timeout, and internal failure when applicable.

### 5. Interface logic for consumers

Explain the behavior without production code:

1. preconditions and authoritative input/context;
2. ordered validation and permission checks;
3. main query/calculation/state-transition steps;
4. database/cache/external calls and transaction boundary;
5. side effects, events, audit/logging, and derived fields;
6. failure, duplicate, concurrency, timeout, and rollback behavior;
7. frontend expectations: loading, disabled/confirmation state, success refresh/navigation, cache invalidation, retry, error display, and whether polling is required.

Keep it concise enough for a frontend engineer to understand what the interface does and how the UI should react. Reference detailed architecture or state-transition sections instead of duplicating algorithms.

### 6. Compatibility and verification

Name existing consumers, backward-compatibility behavior, version/deprecation policy, contract tests, validation tests, permission tests, error tests, and any frontend fixture/mock updates. Every field and outcome must trace to a requirement or necessary response-wrapper convention.

## Contract drafting sequence

Complete every interface in this order. Do not copy a DTO and guess its wire shape.

1. **Prove identity** — combine controller/class mapping, method mapping, context path, gateway prefix, API version, HTTP method, consumes/produces, and real consumer. For RPC/event/job/internal contracts, prove the corresponding protocol identity.
2. **Prove wrappers and errors** — inspect the actual response wrapper, exception handler/error mapping, serialization configuration, enum/date/decimal behavior, and authorization filters/interceptors.
3. **Trace inputs** — identify whether each value comes from Path, Query, Header, Cookie, Body, upload, authenticated principal, tenant context, server clock, configuration, or derived data. Never tell a caller to send a server-derived value.
4. **Trace outputs** — map each output field to database/model/calculation/configuration or dependency response. State masking, sorting, precision, nullability, and stability.
5. **Write behavior** — describe validation and permission order, reads/writes, transaction, side effects, duplicate/concurrency semantics, failures, recovery, and frontend reaction.
6. **Cross-check** — compare the contract with POJO fields, database columns/types, frontend client/types, tests/fixtures, predecessor Specs, compatibility, and rollout.

Use an evidence worksheet before writing the public contract:

| Contract fact | Repository evidence | Confirmed value | Confidence/limit |
| --- | --- | --- | --- |
| Method and application route | Controller mappings + context path | `POST /api/v1/orders` | Static; gateway deployment prefix unverified |
| Success wrapper | Response type + global advice | `ApiResponse<OrderResponse>` | Serialization settings verified in source |
| Error wrapper | Exception handler | `ApiResponse<ValidationErrorData>` | Dependency timeout mapping still needs user decision |
| Consumer | Frontend client symbol | `orderClient.createOrder` | One known consumer; search external consumers separately |

If a route, wrapper, business error, tenant source, compatibility behavior, or externally visible field cannot be proven and different choices materially affect consumers, raise a major decision instead of inventing it.

## Complete HTTP worked example

This example demonstrates required depth only. Its route, wrapper, fields, values, and behavior are fictitious and must never be copied without repository evidence.

### Inventory row

| ID | Name/purpose | Kind | Consumer | Owner | Method + URL | Input | Output | Auth/tenant | Error model | Idempotency/version | Requirements |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `API-021` | Create order | HTTP | Order creation page | Order web module | `POST /api/v1/orders` | Headers + `CreateOrderRequest` | `ApiResponse<OrderResponse>` | Bearer principal; tenant from verified context | Stable HTTP + business codes | `Idempotency-Key`; v1 additive compatibility | `REQ-007`, `REQ-008` |

### API-021 — Create order

#### Identity and purpose

| Concern | Definition |
| --- | --- |
| Purpose/owner/consumer | Create one order for the authenticated tenant; owned by Order module; called by order creation page |
| Protocol and endpoint | `HTTP POST /api/v1/orders` after verified application context path; do not include an environment host |
| Content type/version | Request and response `application/json`; contract version v1 in path |
| Success | HTTP `201 Created`; application code `SUCCESS`; `Location` identifies the created resource when repository convention supports it |
| Auth/permission/tenant | Bearer authentication; permission `order:create`; tenant comes from security context, never request body |
| Timeout/retry/rate limit | Client timeout follows repository client policy; caller retries only network/timeout outcomes using the same idempotency key; no automatic retry for 4xx |
| Idempotency/concurrency | Key is unique within tenant for 24 hours; same key + same normalized payload returns stored outcome; same key + different payload returns conflict |
| Audit/sensitive data | Audit actor, tenant, key hash, result, and correlation ID; never log raw note if classified sensitive |

#### Request parameters

| Name | Location | Type/format | Required/null | Default | Validation/range/enum | Meaning | Example | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `Authorization` | Header | Bearer token | Required; not blank | None | Repository authentication format | Authenticated actor | `Bearer ***` | Security filter |
| `Idempotency-Key` | Header | ASCII string | Required; not blank | None | 16-64 chars; stable across retry; case-sensitive | Identifies one intended create action | `01J5...K9` | Frontend generated once per submission |
| `X-Correlation-Id` | Header | UUID/string per repository | Optional | Server-generated | Must match repository correlation format | End-to-end diagnostic identity | `7fd...` | Caller or server filter |
| `customerId` | Body | 64-bit integer | Required; non-null | None | Positive; customer must be visible to tenant | Customer receiving the order | `12001` | User selection |
| `currency` | Body | uppercase string | Required; non-null | None | One of `CNY`, `USD`; no trim-dependent ambiguity | Currency for all monetary fields | `CNY` | User selection/business context |
| `items` | Body | array of object | Required; non-null | None | 1-100 entries; `productId` unique | Requested lines | See body | Form state |
| `items[].productId` | Body | 64-bit integer | Required; non-null | None | Positive; product visible and orderable | Product identity | `9001` | User selection |
| `items[].quantity` | Body | integer | Required; non-null | None | 1-9999 | Requested units | `2` | Form input |
| `note` | Body | UTF-8 string | Optional; may be absent; `null` rejected if repository validation rejects it | Empty string | Trim; 0-500 chars; control characters rejected | User-visible order note | `Leave at reception` | Form input |

```jsonc
{
  "customerId": 12001, // Required. Tenant-visible customer identifier; positive 64-bit integer.
  "currency": "CNY", // Required. Order currency; allowed values are CNY and USD.
  "items": [ // Required. Unique product lines; array size is 1-100.
    {
      "productId": 9001, // Required. Tenant-visible orderable product identifier; positive 64-bit integer.
      "quantity": 2 // Required. Requested unit count; integer from 1 through 9999.
    }
  ],
  "note": "Leave at reception" // Optional. Trimmed user-visible note; 0-500 characters; empty means no note.
}
```

Cross-field rules: all items use the request currency; duplicate `productId` values are rejected rather than merged; an empty list is invalid; unknown fields follow the repository serializer policy and must be stated explicitly in the real Spec.

#### Success response

HTTP `201 Created`. Show response headers if the repository emits them. The complete illustrative wire payload is:

```jsonc
{
  "code": "SUCCESS", // Stable application success code from the repository response wrapper.
  "message": "ok", // Human/developer-readable success message; not used for programmatic branching.
  "data": { // Non-null created-order payload.
    "orderId": 810001, // Stable created order identifier used for detail navigation.
    "orderNo": "O202608170001", // Tenant-visible immutable order number.
    "status": "CREATED", // Current lifecycle state; CREATED means accepted but not yet fulfilled.
    "currency": "CNY", // Currency shared by all amount fields.
    "totalAmount": "39.80", // Decimal amount serialized as a string with exactly two fractional digits.
    "items": [ // Persisted line projection in deterministic line-number order.
      {
        "productId": 9001, // Product identifier from the accepted request.
        "quantity": 2, // Accepted quantity after validation.
        "unitPrice": "19.90", // Authoritative unit price captured at creation, string decimal with two digits.
        "lineAmount": "39.80" // quantity multiplied by unitPrice using documented rounding.
      }
    ],
    "createdAt": "2026-08-17T10:15:30+08:00" // Server creation time in repository-standard ISO-8601 offset format.
  },
  "traceId": "7fd3c950" // Correlation identifier for support; source is the request/server tracing context.
}
```

| Field path | Type/format | Required/null/default | Validation/enum/precision | Meaning/source | Frontend use |
| --- | --- | --- | --- | --- | --- |
| `data.status` | string enum | Required, non-null | `CREATED`; later states are defined by detail API | State persisted by Service | Show creation success, not fulfillment success |
| `data.totalAmount` | decimal string | Required, non-null | scale 2; repository rounding mode | Sum of persisted line amounts | Display with `currency`; never parse as binary float for calculations |
| `data.createdAt` | ISO-8601 offset timestamp | Required, non-null | Server clock and repository zone rule | Persisted creation time | Localized display only |

#### Error responses

| Condition | HTTP status | Business code | Response shape | Retryable | Frontend handling |
| --- | --- | --- | --- | --- | --- |
| Missing/invalid field or duplicate product | `400` | `ORDER_REQUEST_INVALID` | Wrapper with field violations | No until corrected | Keep form values; mark fields; focus first error |
| Not authenticated | `401` | Repository auth code | Standard auth wrapper | After re-authentication | Start login/session recovery flow |
| Permission denied or tenant mismatch | `403` | `ORDER_CREATE_FORBIDDEN` | Standard error wrapper | No | Show denied state; do not disclose resource details |
| Customer/product absent | `404` or repository-defined business status | `ORDER_REFERENCE_NOT_FOUND` | Standard error wrapper | No until selection changes | Refresh selectable data and show targeted message |
| Same key with different payload | `409` | `IDEMPOTENCY_KEY_CONFLICT` | Standard error wrapper | No with same key | Generate a new key only for a new confirmed user action |
| Dependency timeout before known commit | Repository-defined timeout status | `ORDER_DEPENDENCY_TIMEOUT` | Standard error wrapper | Only per documented rule | Preserve form and offer controlled retry with same key |
| Unexpected failure | `500` | `INTERNAL_ERROR` | Masked standard wrapper | Not automatically | Show generic error with trace ID |

Illustrative validation-error payload:

```jsonc
{
  "code": "ORDER_REQUEST_INVALID", // Stable code for request validation failure.
  "message": "Request validation failed", // Safe summary; frontend branches on code, not message text.
  "data": { // Structured non-null validation details.
    "fieldErrors": [ // One entry per rejected field, ordered by request-field order.
      {
        "field": "items[0].quantity", // Request field path that failed validation.
        "reason": "must be between 1 and 9999", // Safe user-displayable reason or localization source per repository policy.
        "rejectedValue": 0 // Rejected value only when repository privacy policy permits echoing it.
      }
    ]
  },
  "traceId": "7fd3c950" // Correlation identifier for diagnostics.
}
```

#### Interface logic for frontend and consumers

1. The frontend creates one idempotency key when the user begins a confirmed submit and reuses it only for retries of that same normalized request.
2. Authentication, tenant, permission, header, bean, and cross-field validation run in the documented order before business writes; tenant never comes from body data.
3. The Service loads authoritative customer/product data, rejects inaccessible or invalid references, captures price, calculates line/total amounts using repository precision rules, and determines initial state.
4. The Service writes the idempotency record, order, and items inside the documented transaction; the DAO does not decide business state.
5. The operation records audit/correlation data and emits only side effects explicitly designed elsewhere; response fields are mapped from committed state.
6. Duplicate same-payload requests return the stored outcome; different-payload key reuse conflicts; failures roll back or enter the explicitly documented unknown/reconciliation state.
7. The frontend disables duplicate clicks while pending, navigates to the returned order detail on success, invalidates list cache, preserves form data on retryable failure, maps field errors, and never polls unless the contract explicitly returns an asynchronous state.

#### Compatibility and verification

- Consumers: name every frontend client, external client, fixture, mock, and API document found in the repository.
- Compatibility: route, method, wrapper, existing fields, decimal/time serialization, and business codes remain stable; additive fields need repository-defined unknown-field behavior.
- Contract tests: verify status, headers, complete success/error wrapper, enum/date/decimal serialization, and unknown/absent/null fields.
- Validation/security tests: verify every boundary, cross-field rule, permission, tenant isolation, and information-leak behavior.
- Idempotency tests: first request, sequential duplicate, concurrent duplicate, key/payload conflict, expired key, and retry after simulated response loss.
- Frontend tests: submit disabling, success navigation/cache invalidation, field errors, forbidden state, controlled retry, and trace ID display/support behavior.

## List-query expansion example

A list endpoint is a separate interface ID. It is not covered by the create example or a shared `OrderResponse` class name. Its detail must additionally define:

| Parameter | Rule that must be explicit |
| --- | --- |
| `page`/`size` or cursor | base (0/1), default, min/max, invalid handling, stable cursor meaning |
| filters | exact enum/time/null semantics, combination rules, tenant/permission scope |
| sort | allowed fields/directions, default, deterministic tie-breaker, invalid-field behavior |
| search text | trim/case/escaping/tokenization and blank-string behavior |

Illustrative full list response shape:

```jsonc
{
  "code": "SUCCESS", // Stable application success code.
  "message": "ok", // Non-branching result message.
  "data": { // Non-null page payload, including an empty page.
    "items": [ // Ordered result items; empty array when no matches, never null.
      {
        "orderId": 810001, // Stable ID used for row key and detail navigation.
        "orderNo": "O202608170001", // Tenant-visible order number.
        "status": "CREATED", // Current status and filter/display value.
        "totalAmount": "39.80", // Decimal string at repository scale.
        "createdAt": "2026-08-17T10:15:30+08:00" // Server creation timestamp used with orderId as stable sort.
      }
    ],
    "page": 1, // Repository-defined page base; state the base explicitly.
    "size": 20, // Effective page size after validation/defaulting.
    "totalElements": 1, // Total rows matching filters under documented consistency semantics.
    "totalPages": 1, // Derived page count for the effective size.
    "hasNext": false // Whether another page exists at query time.
  },
  "traceId": "7fd3c950" // Correlation identifier.
}
```

State empty-page behavior, count-query behavior/cost, stable ordering, concurrent insert/delete effects, maximum size, field projection, permission filtering, query index, frontend loading/empty/error/pagination state, and list-specific tests.

## Non-HTTP contracts

Apply the same completeness using protocol-appropriate identifiers:

- **RPC**: service/method signature, discovery/routing identity, deadline, retry, metadata/auth propagation, serialization, errors, idempotency, and consumers.
- **Event/message**: topic, key/partition, producer, consumers, envelope/payload, schema version, ordering, delivery guarantee, deduplication, retry/DLQ, replay, compatibility, and observability.
- **Job/CLI/internal service**: exact symbol/command/schedule, caller, inputs, outputs, side effects, concurrency/locking, retry, exit/error semantics, and tests.

Do not fabricate an HTTP URL for a non-HTTP contract.

For every non-HTTP item, still use a stable detailed-subsection shape: identity/ownership, input/envelope, output/acknowledgement, error/retry/DLQ or exit semantics, ordered logic, compatibility/schema evolution, observability, and tests. A Java method signature or topic name alone is only inventory information.

## Depth and consistency gate

Before accepting one contract detail, verify all of the following:

- its six required headings exist in order and contain repository-specific content;
- HTTP identity contains exactly one method and one verified application route;
- every request value has a location, type/format, required/null/default behavior, exact validation, meaning, example, and source;
- request, success, and error bodies are complete where applicable, and every JSON field has a line-end meaning comment;
- success and error transport statuses, stable business codes, retryability, and frontend behavior are explicit;
- consumer logic contains the ordered validation, query/write, transaction, side-effect, failure, duplicate/concurrency, and UI-reaction steps that apply;
- every external field maps to a POJO/model/database/derived source and consumer use with consistent type, precision, enum, nullability, and time rules;
- every interface has named contract, validation, security/tenant, failure, compatibility, and consumer tests.

Do not use a minimum prose length as a substitute for these checks. A short `204 No Content` operation can be complete; a five-page response-class listing can still be incomplete.

## Contract review failures

Return `REVISE` when any applies:

- an inventory contract has no detailed subsection;
- one ID groups several Methods, URLs, RPC methods, or independently callable operations;
- a URL, method, wrapper, field, error, permission, or consumer is inferred without evidence;
- request parameters omit meaning or exact validation behavior;
- response JSON is replaced by a class name, contains `...`, or omits comments on fields;
- frontend-visible logic, state change, side effect, retry, or error handling is unclear;
- interface fields disagree with POJOs, database types/nullability, frontend usage, or tests.
