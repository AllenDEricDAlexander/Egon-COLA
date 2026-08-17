# Per-interface Contract Design

Read this reference whenever Chapter 9 contains an HTTP, RPC, event/message, CLI, scheduled-job, or internal-service contract. An inventory is only the index; every inventory item must have a complete detailed subsection.

## Contents

- [Contract inventory](#contract-inventory)
- [Required per-interface subsection](#required-per-interface-subsection)
- [Non-HTTP contracts](#non-http-contracts)
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

## Non-HTTP contracts

Apply the same completeness using protocol-appropriate identifiers:

- **RPC**: service/method signature, discovery/routing identity, deadline, retry, metadata/auth propagation, serialization, errors, idempotency, and consumers.
- **Event/message**: topic, key/partition, producer, consumers, envelope/payload, schema version, ordering, delivery guarantee, deduplication, retry/DLQ, replay, compatibility, and observability.
- **Job/CLI/internal service**: exact symbol/command/schedule, caller, inputs, outputs, side effects, concurrency/locking, retry, exit/error semantics, and tests.

Do not fabricate an HTTP URL for a non-HTTP contract.

## Contract review failures

Return `REVISE` when any applies:

- an inventory contract has no detailed subsection;
- one ID groups several Methods, URLs, RPC methods, or independently callable operations;
- a URL, method, wrapper, field, error, permission, or consumer is inferred without evidence;
- request parameters omit meaning or exact validation behavior;
- response JSON is replaced by a class name, contains `...`, or omits comments on fields;
- frontend-visible logic, state change, side effect, retry, or error handling is unclear;
- interface fields disagree with POJOs, database types/nullability, frontend usage, or tests.
