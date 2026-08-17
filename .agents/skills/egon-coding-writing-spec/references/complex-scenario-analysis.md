# Complex Scenario Analysis

Read this reference before fixing the architecture conclusion. Its purpose is to prevent a plausible-looking design from being written before the real actors, boundaries, data, failures, and constraints are understood.

## Contents

- [Complexity gate](#complexity-gate)
- [Required analysis for a Complex Spec](#required-analysis-for-a-complex-spec)
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
