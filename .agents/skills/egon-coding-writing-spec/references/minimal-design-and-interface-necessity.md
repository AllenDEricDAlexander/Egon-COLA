# Minimal Design and Interface Necessity

Read this reference before choosing architecture elements or finalizing Chapter 9. Its purpose is to prevent documentation completeness from turning into unnecessary APIs, classes, layers, tables, caches, jobs, or network round trips.

## Contents

- [Smallest-coherent-design rule](#smallest-coherent-design-rule)
- [Element-necessity audit](#element-necessity-audit)
- [Interface-necessity test](#interface-necessity-test)
- [Parameter ownership and derivation](#parameter-ownership-and-derivation)
- [Fetch-then-forward anti-pattern](#fetch-then-forward-anti-pattern)
- [Legitimate discovery and selection](#legitimate-discovery-and-selection)
- [Interaction-cost and race analysis](#interaction-cost-and-race-analysis)
- [Worked decisions](#worked-decisions)
- [Final simplicity gate](#final-simplicity-gate)

## Smallest-coherent-design rule

Start with the smallest repository-consistent option that satisfies the user-visible requirements and preserves correctness. The baseline option must reuse existing contracts, context, components, models, and infrastructure wherever their semantics already fit.

A more complex option may be selected only when the Spec identifies a concrete requirement or constraint that the simpler option cannot satisfy. “Cleaner,” “more flexible,” “future-proof,” “decoupled,” or “best practice” are not sufficient without a present variation point and evidence.

Use this dominance rule:

```text
If option A satisfies the same approved requirements as option B
and A adds fewer contracts, states, dependencies, network calls, failure points, or migration obligations,
select A unless B has a proven material advantage required now.
```

Do not add scope merely because the template has a chapter for it. Detailed documentation describes the selected solution; it does not justify making the solution larger.

## Element-necessity audit

Inventory every proposed new or materially expanded design element before drawing the target architecture:

| Proposed element | Change | Requirements | Existing/direct alternative | Concrete inadequacy of alternative | Added costs and failure modes | Verdict |
| --- | --- | --- | --- | --- | --- | --- |
| `<API/class/table/cache/job/layer/dependency>` | New / Expand / Keep / Remove | `REQ-001` | `<reuse/derive/direct path>` | `<evidence-backed gap or None>` | `<coupling/state/RTT/ops/migration>` | Add / Keep / Merge / Remove |

Apply the audit to:

- public and internal HTTP/RPC/event contracts;
- metadata, selector, lookup, validation, preview, and capability endpoints;
- Service interfaces and implementations, helper components, adapters, factories, strategies, and facades;
- Request/Response/DTO/VO/BO/PO/Entity objects and mappers;
- tables, columns, indexes, caches, queues, scheduled jobs, configuration, feature flags, and dependencies;
- frontend stores, providers, hooks, routes, pages, dialogs, and preflight requests.

For an existing element, say `Keep` and cite its current consumer and responsibility. For a new element, the inadequacy and costs columns cannot be empty.

## Interface-necessity test

Before assigning a new interface ID, answer all of the following:

1. **Independent goal** — does the operation deliver an independently observable user/system outcome, or is it only a mechanical preparation step for another operation?
2. **Real consumer** — name the current or approved consumer. Does it use the result for display, choice, caching, validation, workflow branching, or another independent purpose?
3. **Authoritative owner** — which component owns the returned or accepted fact, and does that fact change independently of the target operation?
4. **Direct alternative** — can an existing endpoint, a target-endpoint extension, an authenticated context, route/resource identity, local build-time data, or direct server-side lookup satisfy the requirement?
5. **Derivation** — can the target backend derive the value safely from principal, tenant context, current resource, persisted relationship, configuration, or another server-owned fact?
6. **Caller knowledge** — does the caller already possess the value from user input, route state, current record, local registry, build artifact, or a previous business operation?
7. **Result usage** — will the caller do anything meaningful with the result other than copy it unchanged into the next request?
8. **Interaction cost** — how many round trips, loading/error states, cache entries, retries, permissions, tests, and operational signals are added?
9. **Race semantics** — can the discovered value change between fetch and use? Which operation revalidates it authoritatively?
10. **Lifecycle** — does the interface have an independent versioning, compatibility, ownership, and deprecation lifecycle?

Default to `Merge`, `Reuse`, or `Remove` when the operation has no independent outcome and the target operation can derive or validate the value itself.

## Parameter ownership and derivation

For every nontrivial request parameter, decide transport only after ownership:

| Parameter | Semantic owner | Caller already knows it? | Target can derive it? | User must choose it? | Independent variability | Transport decision |
| --- | --- | --- | --- | --- | --- | --- |
| `<field>` | `<user/frontend/backend/external>` | Yes / No | Yes / No, how | Yes / No | `<when it changes>` | Body / Path / Context-derived / Remove |

Rules:

- identity, tenant, permissions, and service-source facts already authenticated by infrastructure stay server-side unless the contract explicitly needs a user-selected scope;
- do not return a server-derived value through API A merely so the caller can send it back through API B;
- do not fetch build-time constants, local route metadata, fixed enums, or data already present in the current record merely to populate another request;
- do not make a client choose an internal database ID when a stable business key or current-resource relationship is the real contract;
- a target operation must revalidate mutable server-owned facts even when a preceding selector or discovery operation displayed them;
- do not hide a major ownership decision inside a “parameter source” row.

## Fetch-then-forward anti-pattern

Reject this by default:

```text
Frontend -> GET /operation-context
Backend  -> { tenantId, applicationId, sourceCode }
Frontend -> POST /execute { tenantId, applicationId, sourceCode, businessInput }
Backend  -> executes after re-reading the same context
```

It adds latency, two loading/error paths, stale-value races, duplicated authorization, extra client state, and another compatibility surface without changing the user's goal.

Prefer one of these:

1. **Backend derivation**

```text
Frontend -> POST /execute { businessInput }
Backend  -> derives tenant/application/source from authenticated or persisted context
```

2. **Current-resource route**

```text
Frontend -> POST /applications/{applicationId}/execute { businessInput }
```

Use this only when `applicationId` already identifies the page/resource or is a real user selection; do not add a preflight solely to obtain it.

3. **Combined command**

```text
Frontend -> POST /execute { stableBusinessKey, businessInput }
Backend  -> resolves internal IDs and executes atomically
```

4. **Local/build-time source**

Use a typed local registry/configuration when the values ship with the frontend and do not require server-authoritative discovery.

The target operation remains responsible for authorization and validation; a preceding query never makes an unsafe command safe.

## Legitimate discovery and selection

A separate query may be justified when it has standalone consumer value, for example:

- the user must choose from a permission-filtered, server-owned, independently changing catalog;
- the UI displays search, labels, availability, status, price, capacity, or other decision information rather than forwarding an opaque parameter;
- several independent operations reuse a cacheable/versioned catalog;
- capability or protocol negotiation changes client behavior and has explicit version/expiry semantics;
- pagination/search makes embedding the options in a command impossible or wasteful;
- an operator reviews or audits the discovered state as a separate use case.

When justified, document:

- the independent use case and consumer-visible result;
- why local/current/request context is insufficient;
- cache and invalidation/expiry behavior;
- selection identity and stable business meaning;
- command-time revalidation and stale-selection error;
- whether cancellation or query failure blocks the user goal;
- why combining the operations is worse.

“The frontend needs the ID” is not an independent use case.

## Interaction-cost and race analysis

Compare the critical user path before and after the design:

| Path | Network calls | New client states | New server contracts/state | Failure/race points | User-visible value |
| --- | --- | --- | --- | --- | --- |
| Direct baseline | `<count>` | `<loading/error>` | `<elements>` | `<points>` | `<outcome>` |
| Proposed design | `<count>` | `<states>` | `<elements>` | `<points>` | `<additional value>` |

Count only business-path calls, not static asset loading. Name retries and cache hits separately. Reject a proposal that increases calls and failure points without an approved correctness, security, usability, or operational benefit.

For a fetch-then-command sequence, define TOCTOU behavior:

```text
T1 query returns option/version V
T2 authoritative state changes to V+1
T3 command submits V
T4 command must reject, refresh, or apply an explicitly allowed version rule
```

Never rely on the preflight response as command-time authorization.

## Worked decisions

### Rejected mechanical parameter endpoint

| Concern | Decision |
| --- | --- |
| Change classification | Proposed new `GET /current-application-context` |
| Independent consumer goal | None; result is not displayed or selected |
| Parameter ownership and derivation | Tenant/application are already present in authenticated principal and persisted route resource |
| Direct/no-new-interface alternative | Target command derives both values and accepts only business input |
| Caller use of result | Copies fields unchanged into the command |
| Round trips and failure points | Adds one RTT, loading/error state, cache/race, auth/test surface |
| Verdict | `Remove` — backend derivation dominates |

### Accepted user-visible selector

| Concern | Decision |
| --- | --- |
| Change classification | Reuse existing paged application catalog query |
| Independent consumer goal | Administrator searches and chooses an ACTIVE application using name/status/owner data |
| Parameter ownership and derivation | Catalog is server-owned and changes independently; command cannot infer user choice |
| Direct/no-new-interface alternative | Embedding the entire catalog in the command/page route is stale and unbounded |
| Caller use of result | Displays options and records an explicit human selection |
| Round trips and failure points | One cacheable query; command revalidates selection and reports stale/inactive conflict |
| Verdict | `Keep` — independent selection value is proven |

### Rejected speculative abstraction

| Proposed element | Existing/direct alternative | Evidence | Verdict |
| --- | --- | --- | --- |
| `ExecutionStrategyFactory` with one strategy | Direct Service method using existing collaborators | No present variation point or second implementation | Remove |
| New cache for a bounded local lookup | Existing indexed database query | No latency/capacity evidence and invalidation adds state | Remove |
| New response DTO identical to an internal stable response model | Reuse the existing contract type | Same ownership, exposure, validation, lifecycle, and version | Reuse |

## Final simplicity gate

Before the Spec can pass review:

- every proposed element has one requirement or necessary infrastructure rationale and an explicit `Add/Keep/Merge/Remove` verdict;
- the direct/no-new-element baseline was evaluated first;
- no interface exists solely to return parameters that another interface can derive or that the caller forwards unchanged;
- every new interface has an independent consumer goal or a proven discovery/selection/negotiation reason;
- every new class passes the class-necessity test; every pattern has a present variation point;
- interaction counts, loading/error states, failure points, cache/invalidation, and TOCTOU behavior are visible;
- speculative future extensibility does not outweigh present complexity;
- the selected design contains the fewest moving parts among options satisfying the approved requirements.

Return `REVISE` when a more direct design satisfies the same requirements with fewer contracts or states and the Spec provides no material reason to reject it.
