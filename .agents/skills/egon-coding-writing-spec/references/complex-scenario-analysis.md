# Complex Scenario Analysis

Read this reference before fixing the architecture conclusion. Its purpose is to prevent a plausible-looking design from being written before the real actors, boundaries, data, failures, and constraints are understood.

## Contents

- [Complexity gate](#complexity-gate)
- [Required analysis for a Complex Spec](#required-analysis-for-a-complex-spec)
- [Operational analysis passes](#operational-analysis-passes)
- [Worked example of a defensible conclusion](#worked-example-of-a-defensible-conclusion)
- [Depth and quality gate](#depth-and-quality-gate)
- [Required diagrams](#required-diagrams)
- [Complex-design stop conditions](#complex-design-stop-conditions)

## Complexity gate

Classify the Spec as `Complex` when any material driver exists:

- three or more participating modules, services, actors, or deployment processes;
- a public HTTP/RPC/event contract with multiple consumers or compatibility obligations;
- schema migration, backfill, historical-data repair, or more than one source of truth;
- authorization, tenancy, privacy, audit, or cross-identity propagation;
- distributed transaction, concurrency, ordering, deduplication, idempotency, cache consistency, or financial correctness;
- synchronous and asynchronous paths for the same business outcome;
- several roles/pages/user journeys or a multi-stage lifecycle/state machine;
- external systems, partial failure, retry, timeout, failover, rollout, or cross-team ownership;
- conflicting predecessor Specs, competing architecture choices, or an irreversible decision.

Otherwise classify it as `Simple`. Record `Complexity` and `Complexity Drivers` in the header. Complexity is about decision risk and interaction count, not the number of files.

For a Simple Spec, keep the same evidence, contract, file, database, test, and review quality, but do not manufacture matrices, alternatives, or abstractions that add no information.

## Required analysis for a Complex Spec

Complete these artifacts before selecting the final architecture.

### 1. Evidence and current-chain map

Trace from each real entry point to its consumers and side effects:

| Entry/trigger | Current call chain | Data read/written | External dependency | Consumers | Evidence |
| --- | --- | --- | --- | --- | --- |

Include registration/discovery paths, configuration, persistence, events, frontend callers, tests, and predecessor decisions when applicable. Mark static evidence, inference, and unverified runtime behavior separately.

### 2. Scenario matrix

Write one row for each materially different journey, including failure and recovery paths:

| Scenario | Actor/trigger | Preconditions | Main path | Alternative/failure path | Data/state change | Observable result | Requirements |
| --- | --- | --- | --- | --- | --- | --- | --- |

Do not hide retry, duplicate request, timeout, stale data, partial success, permission denial, empty data, cancellation, rollback, or concurrent update inside a generic “error handling” paragraph.

### 3. Boundary and ownership map

For every module/component/data store, state:

- the capability and data it owns;
- authoritative source of truth;
- accepted inputs and emitted outputs;
- permitted dependencies and forbidden knowledge;
- transaction and consistency boundary;
- caller/consumer compatibility obligations.

If two components appear to own the same decision or data, resolve it or raise a major decision.

### 4. Quality-attribute and constraint matrix

| Concern | Required behavior/SLO | Current evidence | Design mechanism | Failure/degradation behavior | Verification |
| --- | --- | --- | --- | --- | --- |

Cover only applicable concerns, but explicitly consider security, tenancy, correctness, consistency, latency, throughput/capacity, availability, recoverability, observability, compatibility, operability, and maintainability.

### 5. Alternatives and trade-offs

Compare only materially viable alternatives. For each, show repository fit, contract/data impact, failure behavior, migration cost, operational burden, and reversibility. Do not create a ceremonial second option.

### 6. Conclusion chain

Every material conclusion must be expressible as:

```text
Repository/User Evidence
  -> Constraint or Requirement
  -> Design Decision
  -> Consequence and Trade-off
  -> Verification and Acceptance Evidence
```

Reject conclusions based only on familiarity, generic best practice, or a class/package list. If the chain contains an unapproved major assumption, stop and ask the user.

## Operational analysis passes

Use the following passes in order. Do not start the target architecture diagram during Pass 1 or 2; an early diagram anchors the design before the evidence and failure modes are known.

### Pass 1: turn the request into decision questions

For each `REQ-*`, list the questions the architecture must answer. A useful question changes the design if answered differently.

| Requirement | Decision question | Why it changes the design | Evidence needed | Major or small gap |
| --- | --- | --- | --- | --- |
| `REQ-001` | Which component is the authoritative writer? | Determines transaction, API, and recovery ownership | Existing write call chain, table owner, predecessor decision | Major |

At minimum ask about entry/consumer, ownership, source of truth, validation, permission/tenant context, transaction, consistency, failure/recovery, compatibility, observability, and acceptance evidence. Do not convert unanswered major questions into assumptions.

### Pass 2: build an evidence ledger before conclusions

Classify every material statement so an inference cannot masquerade as a repository fact:

| Evidence ID | Classification | Path/symbol/decision | Observed fact | Supports | Limit or freshness |
| --- | --- | --- | --- | --- | --- |
| `EVD-001` | Static repository evidence | `module/path:Symbol` | The entry delegates to a Service interface | `REQ-001`, boundary decision | Does not prove runtime registration |
| `EVD-002` | User decision | `<linked wording>` | The operation must remain backward compatible | compatibility decision | Applies only to named scope |
| `EVD-003` | Inference | Existing sibling convention | New handler likely uses the same wrapper | candidate interface design | Confirm before public-contract finalization |
| `EVD-004` | Runtime evidence | `<command/log/date>` | Registered route resolves to the target controller | routing conclusion | Environment-specific and time-bounded |

Inspect at least the entry registration, consumer, orchestration, persistence/external side effects, configuration, error mapping, tests, and predecessor Specs that apply. Record missing evidence explicitly; absence of evidence is not proof of absence.

### Pass 3: trace the current chain and the target delta

Write the current chain before the target chain. For every hop capture input, output, state effect, failure, ownership, and proof. Then describe the exact delta rather than replacing the current system with an idealized architecture.

| Step | Current hop | Input/output | State or side effect | Failure behavior | Owner | Evidence | Target delta |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Frontend -> Controller | Request JSON -> wrapper | None | Validation wrapper | Web module | `EVD-*` | Add one field; keep route |
| 2 | Controller -> Service | Request -> command | None | Business exception mapping | Biz module | `EVD-*` | Preserve interface boundary |

Trace registration/discovery and asynchronous consumers separately from the request path. A producer call ending successfully does not prove that a consumer is registered, that delivery succeeds, or that the final state changes.

### Pass 4: expand scenarios systematically

Start with the business outcome, then vary one material dimension at a time:

- actor/role/tenant and permission result;
- missing, empty, invalid, boundary, stale, or conflicting input;
- resource absent, already in target state, or concurrently changed;
- first request, duplicate request, retry after unknown outcome, and replay;
- dependency success, business rejection, timeout, transport error, and recovery;
- transaction success, partial write, rollback failure, and reconciliation;
- old/new caller or application version during rollout;
- frontend initial/loading/success/empty/error/disabled/denied behavior.

Merge rows only when preconditions, state changes, response, recovery, and acceptance evidence are identical. “Handle errors” is never a scenario.

### Pass 5: resolve ownership, consistency, and failure semantics

Use both an ownership table and a failure analysis. The owner is the component authorized to decide or mutate the fact, not merely the component currently holding a copy.

| Fact/decision | Authoritative owner | Readers/copies | Write entry | Consistency model | Conflict rule | Recovery owner |
| --- | --- | --- | --- | --- | --- | --- |
| `<state>` | `<component/table>` | `<cache/consumer>` | `<API/service>` | Strong / eventual | `<version/order rule>` | `<component/runbook>` |

| Failure point | Detection | Immediate behavior | Data state | Retry/idempotency | User-visible result | Reconciliation |
| --- | --- | --- | --- | --- | --- | --- |
| `<dependency timeout>` | `<exception/metric>` | `<rollback/degrade>` | `<committed/uncommitted/unknown>` | `<key and limit>` | `<documented error>` | `<job/manual repair>` |

If the design says “retry,” state who retries, what is retried, the maximum/backoff, the idempotency identity, what happens after exhaustion, and how operators observe it.

### Pass 6: evaluate alternatives against the same evidence

Compare the direct repository-consistent option with every materially different viable option. Score words such as High/Medium/Low are insufficient without explanation.

| Criterion | Weight or priority | Option A evidence | Option B evidence | Decision effect |
| --- | --- | --- | --- | --- |
| Contract compatibility | Must | Keeps existing route and wrapper | Requires caller migration | Reject B unless user approves breaking change |
| Failure atomicity | Must | One local transaction | Cross-service compensation | Prefer A for current scope |
| Operational cost | Should | Existing metrics/runbook | New queue and DLQ | A has lower rollout burden |

Do not create a second option that is knowingly incompatible or nonsensical just to make the selected option look better.

### Pass 7: derive conclusions and run contradiction checks

Write each material conclusion as a table row, not as an unsupported adjective such as “more scalable” or “more elegant.” Then check:

- each requirement appears in at least one scenario and one conclusion or direct design rule;
- every diagram participant exists in the boundary/file/interface design;
- every state change has one owner, transaction/consistency rule, failure result, and test;
- every public field has the same meaning, type, nullability, enum, and source across interface, POJO, database, frontend, and tests;
- every retry has idempotency and terminal behavior;
- every compatibility promise has a rollout and verification action;
- every claimed SLO has a measurable mechanism and validation boundary.

## Worked example of a defensible conclusion

The following example is illustrative only. Replace all names with repository evidence; never copy `Order`, routes, statuses, or technology choices into an unrelated Spec.

### Example evidence and current chain

| Evidence ID | Classification | Observed fact | Design significance |
| --- | --- | --- | --- |
| `EVD-101` | Static | `OrderController#create` calls `OrderService#create`; the Controller does not access DAO | Preserve the Controller -> Service boundary |
| `EVD-102` | Static | `OrderServiceImpl#create` writes `orders` and `order_items` in one local transaction | The service implementation is the transaction owner |
| `EVD-103` | Static | The frontend retries after a network failure and currently sends no idempotency key | Duplicate order creation is a real failure path |
| `EVD-104` | User decision | Existing `POST /api/orders` consumers must remain compatible | Keep route and wrapper; additive fields only |

### Example scenario slice

| Scenario | Preconditions | Path | State effect | Observable result | Design pressure |
| --- | --- | --- | --- | --- | --- |
| First valid submission | Permission and stock valid | Validate -> transaction -> response | One order and its items committed | Success wrapper with order ID | Ordinary atomic write |
| Invalid item quantity | Quantity is zero | Reject before DAO | No write | Stable validation code | Validation order must be explicit |
| Permission denied | Actor lacks create permission | Reject before business query | No write | Permission error; no detail leakage | Security before resource disclosure |
| Duplicate submission | Same idempotency key and same payload | Return stored outcome | No second order | Same business result | Persist key/result identity |
| Key reused with different payload | Same key, different payload hash | Reject conflict | No second order | Conflict code | Hash comparison is required |
| Commit outcome unknown to client | Database committed; response lost | Client retries | Existing order retained | Stored success returned | Retry must be safe after unknown outcome |

### Example ownership and quality slice

| Fact/concern | Owner or requirement | Mechanism | Failure behavior | Verification |
| --- | --- | --- | --- | --- |
| Order identity/state | `orders`, written by `OrderServiceImpl` | One local transaction with items | Roll back both tables on write failure | Integration test verifies zero partial rows |
| Duplicate identity | Idempotency record scoped to tenant + key | Unique constraint and request hash | Conflict on key/hash mismatch | Concurrent duplicate test |
| Backward compatibility | Existing HTTP consumer contract | Keep route/wrapper; add optional response metadata only | Old clients ignore additive field | Contract fixture for old response parser |
| Observability | Operation support | Correlation ID plus result/latency/error metrics | Alert on failure ratio; audit stable outcome | Metrics assertion or documented runtime gap |

### Example alternatives

| Option | Fit | Failure/consistency impact | Migration/operations | Decision |
| --- | --- | --- | --- | --- |
| A. Keep orchestration in existing Service and add local idempotency storage | Matches current transaction and ownership | Atomic order/items/idempotency result | One migration; no new runtime | Selected for current scope |
| B. Publish a create event and return immediately | Changes synchronous success semantics | Eventual creation, DLQ/replay required | New broker contract and frontend polling | Rejected because it conflicts with `EVD-104` and adds unrequested operations |

### Example conclusion chains

| Conclusion | Evidence | Constraint/requirement | Decision | Consequence/trade-off | Verification |
| --- | --- | --- | --- | --- | --- |
| Service remains orchestration and transaction owner | `EVD-101`, `EVD-102` | Preserve repository boundary and atomic write | Extend `OrderServiceImpl`; Controller still depends on Service | Smallest change; Service owns extra duplicate logic | Unit orchestration tests plus transaction integration test |
| Requests become safely retryable | `EVD-103` | One user action must not create duplicates | Persist tenant-scoped idempotency key, payload hash, and stable result in the transaction | Additional row/index and retention policy | Sequential and concurrent duplicate tests; conflict test |

These are defensible because the decisions follow evidence and state a cost. “Use an idempotency pattern because it is best practice” is not defensible.

## Depth and quality gate

A Complex Spec is not ready for architecture selection until it normally contains:

- two or more evidence/current-chain rows covering different boundaries;
- three or more materially different scenarios, including at least one failure/recovery case;
- explicit ownership for every mutable fact and important decision;
- three or more applicable quality/constraint rows;
- a failure row for every external or asynchronous boundary and every unknown commit outcome;
- at least two conclusion chains from different decision classes;
- an honest boundary between static proof and runtime behavior not verified.

Fewer rows are allowed only when repository evidence proves fewer real elements exist and the affected subsection records `Depth exception:` followed by that exact evidence. More rows do not improve quality when they repeat the same path with renamed wording.

## Required diagrams

For a Complex Spec, Chapter 7 must contain all three Mermaid views:

1. **Architecture view** — `flowchart` showing actors, components, stores, external systems, trust/deployment boundaries, and dependency direction.
2. **Business/control flow** — a separate `flowchart` showing decisions, alternative branches, errors, retries, and terminal outcomes for the critical use case.
3. **Swimlane view** — a Mermaid `sequenceDiagram` used as the swimlane, showing the ordered interaction between actor/frontend/controller/service/DAO/database/external systems, including key failure or rollback behavior.

Label nodes with real modules/contracts and reference `REQ-*`, interface IDs, table names, or state names where useful. Diagrams must agree with the prose and must not show an unimplemented shortcut such as Controller-to-DAO access.

For a Simple Spec, use the views that materially improve understanding. Mark an omitted view `N/A` with the exact reason; never insert a decorative diagram.

## Complex-design stop conditions

Do not finalize the architecture when any of these remains unresolved:

- a critical actor, consumer, source of truth, or write owner is unknown;
- the happy path is known but partial failure, retry, concurrency, or rollback semantics are not;
- an interface field cannot be traced to its model/database/frontend use;
- an index has no identified query, or a query has no credible access path;
- security/tenant context changes across a boundary without an explicit propagation rule;
- the chosen design contradicts a current contract or accepted Spec without an amendment decision;
- the conclusion cannot be supported by the evidence-to-verification chain.
