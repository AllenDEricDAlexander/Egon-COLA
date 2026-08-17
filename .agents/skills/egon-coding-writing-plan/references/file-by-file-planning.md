# Detailed File-by-file Planning

Read this reference before writing Chapter 4, Chapter 5, or any implementation Step. A Plan is executable only when an implementer can follow the stated file order, pseudocode, intermediate states, commands, and commit scopes without making architecture or business decisions.

## Contents

- [Executability standard](#executability-standard)
- [Pre-plan Spec sanity audit](#pre-plan-spec-sanity-audit)
- [Step boundary design](#step-boundary-design)
- [Dependency and file-order algorithm](#dependency-and-file-order-algorithm)
- [Required per-file design](#required-per-file-design)
- [Pseudocode quality bar](#pseudocode-quality-bar)
- [Backend worked Step](#backend-worked-step)
- [Frontend worked Step](#frontend-worked-step)
- [Database migration worked Step](#database-migration-worked-step)
- [Validation ladder](#validation-ladder)
- [Commit and handoff contract](#commit-and-handoff-contract)
- [Final detail gate](#final-detail-gate)

## Executability standard

A Step is not detailed because it contains many words. It is detailed when all of these are unambiguous:

- exact observable outcome and covered requirements;
- baseline repository state and dependencies already committed;
- test-first applicability and expected RED reason;
- exact ordered files, operations, symbols, and why each file occurs at that point;
- current repository evidence for every modified symbol and style choice;
- signatures, fields, mappings, branches, errors, transactions, side effects, and consumers;
- intermediate state after each file, including whether compilation or a test is intentionally red;
- exact validation working directory, command, expected exit/result, and failure return point;
- exact commit paths, message, rollback/forward-fix point, and Step end state.

Reject “implement service,” “update API,” “add frontend,” “write tests,” or copied Spec prose. The Plan must explain how the repository changes, not repeat what the feature does.

## Pre-plan Spec sanity audit

Before turning Spec elements into files, verify that the effective Spec's design is necessary and internally executable.

| Spec element | Spec necessity verdict/evidence | Current repository evidence | Direct/reuse alternative | Plan decision |
| --- | --- | --- | --- | --- |
| `<API/class/table/cache/job/page/dependency>` | `<Spec section>` | `<path/symbol/consumer>` | `<reuse/derive/merge>` | Implement / Already exists / Return to Spec |

Return `REVISE` or `BLOCKED` to the Spec/user when:

- a new interface only fetches parameters copied unchanged into another request;
- a target backend can derive identity, tenant, application, source, or ownership from existing context;
- an added class/layer/table/cache/pattern has no current variation point or requirement;
- the Spec's “direct alternative” is absent, ceremonial, or actually satisfies the same requirements with fewer moving parts;
- a proposed consumer, path, symbol, query, migration, page, or validation command does not exist and the Spec did not approve creating it;
- implementation would require inventing a contract, state, error, field mapping, transaction, or compatibility rule.

Do not repair the architecture inside the Plan. Record the evidence and exact Spec sections that require amendment. Planning detail must never make an unjustified design look implementation-ready.

## Step boundary design

One Step should produce one independently verifiable semantic outcome and normally one commit. A Step may modify several files when they form one inseparable RED/GREEN slice.

Good Step boundaries:

- one contract behavior from focused RED test through implementation and wiring;
- one schema migration plus the minimum mapping/repository compatibility required to validate it;
- one frontend user action from component test through client/state/render behavior;
- one mechanical repository-wide migration when every path changes identically and one validation proves it.

Bad Step boundaries:

- “Backend,” “Frontend,” or “Tests” with several unrelated behaviors;
- one Step per file when intermediate commits cannot compile or prove anything;
- an entire feature in one Step despite separable requirements and tests;
- a Step that creates architecture needed only by later unapproved work;
- a Step whose files overlap another Step without naming the exact later reason and ownership.

For every Step state:

| Concern | Required content |
| --- | --- |
| Baseline state | What exists and passes before edits; prior Step commits required |
| Observable outcome | One externally or technically verifiable result |
| End state | Exact contracts/files/tests now available and what remains intentionally absent |
| Test-first gate | `Required` with RED reason, or `Not applicable` with repository/technical evidence |
| Commit scope | Exact paths owned by the Step; no unrelated files |

## Dependency and file-order algorithm

Derive order rather than applying a fixed layer list.

1. **Identify the proof point** — choose the focused unit/contract/component/migration test that proves the missing behavior.
2. **Identify compile prerequisites** — types, generated contracts, fixtures, schema, or shared constants needed for the test to compile. If these must precede RED, explain the intentional order.
3. **Place the RED file** — create/modify the focused test before the behavior implementation and state the expected failure.
4. **Add the minimum implementation path** — order types, mapping, persistence, business logic, entry point, and consumer by actual compile/runtime dependencies.
5. **Add wiring and registration** — only after the implementation exists; name configuration, generated registries, routes, dependency injection, or exports.
6. **Add compatibility/operational files** — migrations, config, permission manifests, metrics, docs, or rollout scripts exactly where their dependency becomes valid.
7. **Close with verification** — focused GREEN first, then the smallest module/cross-module gate required for the Step.

Typical orders are guidance, not rules:

```text
Java behavior:
focused test -> contract/data type -> DAO/mapper -> service.impl -> controller/wiring -> focused + module tests

Frontend behavior:
component/hook test -> API/type contract -> client/query hook -> component/page -> route/wiring -> focused + typecheck

Database change:
migration contract test -> new migration -> PO/mapper -> DAO query -> service compatibility -> migration + integration test

Generated RPC:
proto/IDL contract test -> IDL -> generation command/output -> provider -> client adapter -> consumer test
```

When compilation requires a contract before its test, state that File 1 establishes only the compile prerequisite and File 2 is the RED behavior gate. Never pretend the contract file itself proves RED.

## Required per-file design

Every `#### File N` block must contain:

| Field | Required detail |
| --- | --- |
| Purpose | One responsibility in this Step, not the whole feature |
| Symbols | Exact classes/methods/functions/types/fields/config keys/test cases |
| Repository evidence | Existing path/symbol/pattern proving placement and style |
| Dependencies and consumers | Callers, callees, imports/modules, runtime/compile consumers |
| Why now | Dependency reason for this exact position |
| Contract/signature changes | Exact before/after or new signature/field/annotation/schema |
| Input/output and state mapping | Field-by-field source/target, defaults, nulls, state/persistence effects |
| Error and edge behavior | Validation, permission, missing, duplicate, concurrent, dependency, rollback branches |
| Implementation pseudocode | Repository-language algorithm with real symbols |
| Verification contribution | Which test/gate observes this file's behavior |
| After this file | Compile/RED/GREEN/wired state and known remaining gap |

For `DELETE`, explain consumers already removed, replacement if any, search proof, generated references, compatibility, and how validation proves absence. For `RENAME`, name both paths and every reference update. For `GENERATED`, name the source-of-truth file and exact generation command; never hand-edit generated output.

## Pseudocode quality bar

Pseudocode must be close enough to implementation that an executor chooses syntax details, not behavior or architecture.

Include as applicable:

- exact annotation, visibility, class/interface, method/function signature, parameter and return types;
- validation order and exact error/exception/result mapping;
- authoritative context derivation rather than caller-supplied security facts;
- collaborator calls in order and transaction boundary;
- query predicates, joins, ordering, pagination, affected-row expectations, locks, and indexes;
- field mapping, null/default/enum/time/decimal conversions, sensitive-data handling;
- idempotency, duplicate, concurrency, retry, timeout, rollback, cache invalidation, and events;
- frontend state transitions, hooks, query keys, loading/empty/error/disabled branches and user actions;
- test fixtures, action, assertions, mocks/fakes, persisted/published effects, and negative assertions.

Pseudocode must not:

- invent symbols missing from both Spec and repository;
- repeat `validate`, `process`, `save`, or `handle error` without naming conditions and outcomes;
- contain production-ready full method bodies that belong in implementation;
- hide an unresolved choice behind “as appropriate,” “if needed,” or “etc.”;
- introduce a helper, mapper, factory, strategy, endpoint, or cache without a Spec necessity verdict.

## Backend worked Step

Illustrative structure only; replace every symbol/path/command with repository evidence.

### Step — Reject duplicate order creation atomically

- Requirements: `REQ-007`, `REQ-008`
- Dependencies: Migration Step committed
- Baseline state: `OrderServiceImpl#create` writes orders/items but has no tenant-scoped idempotency lookup; focused tests pass for ordinary create.
- Observable outcome: same tenant/key/payload returns the first result; different payload conflicts; no duplicate order is written.
- End state: Service and DAO implement the approved idempotency contract; Controller contract is unchanged; concurrency integration remains for the next declared file in this Step.
- Test-first gate: `Required` — focused duplicate test currently creates two orders or cannot find stored result.
- Ordered files:

#### File 1 — `MODIFY src/test/java/.../OrderServiceImplTest.java`

- Purpose: Define sequential replay and key/hash conflict before implementation.
- Symbols: `returnsStoredResultForSameKey`, `rejectsSameKeyForDifferentPayload`
- Repository evidence: existing create fixtures and repository fake in this test class.
- Dependencies and consumers: invokes public `OrderService#create`; observes fake DAO writes and returned `OrderResult`.
- Why now: establishes the RED contract without changing production behavior.
- Contract/signature changes: reuse existing create signature and add idempotency key to the repository-approved command only if already specified.
- Input/output and state mapping: tenant from test security context; canonical request -> hash; stored result -> returned result.
- Error and edge behavior: same hash returns first result; different hash throws `IdempotencyConflictException`; both assert one order write.
- Implementation pseudocode:

```java
@Test same_key_same_payload_returns_first_result() {
    arrange tenantContext(TENANT_A), command(KEY_1, PAYLOAD_A)
    first = service.create(command)
    replay = service.create(command)
    assertThat(replay).isEqualTo(first)
    verify(orderDao, times(1)).insert(any())
}

@Test same_key_different_payload_conflicts_without_second_write() {
    service.create(command(KEY_1, PAYLOAD_A))
    assertThatThrownBy(() -> service.create(command(KEY_1, PAYLOAD_B)))
        .isInstanceOf(IdempotencyConflictException.class)
    verify(orderDao, times(1)).insert(any())
}
```

- Verification contribution: RED command targets these two tests and must fail for missing replay/conflict behavior.
- After this file: tests compile against the approved contract and fail for the stated behavior, not fixture setup.

#### File 2 — `MODIFY src/main/java/.../service/impl/OrderServiceImpl.java`

- Purpose: Own canonical hash comparison and transaction orchestration.
- Symbols: `create(CreateOrderCommand)`, composed `OrderIdempotencyService/Dao` only when approved by Spec.
- Repository evidence: existing `@Transactional` create method and exception mapping convention.
- Dependencies and consumers: Controller depends on `OrderService`; implementation calls idempotency/order/item DAO.
- Why now: File 1 fixes behavior; this is the minimum GREEN orchestration.
- Contract/signature changes: no Controller route change; use approved command/key fields.
- Input/output and state mapping: derive tenant from trusted context; canonical business fields -> request hash; persisted first result -> response.
- Error and edge behavior: same hash replay; different hash conflict; unique race reloads winner; transaction failure writes no partial order/result.
- Implementation pseudocode:

```java
@Transactional
OrderResult create(CreateOrderCommand command) {
    tenantId = currentTenant.requireId()
    hash = requestCanonicalizer.sha256(command.businessFields())
    existing = idempotencyDao.find(tenantId, command.key())
    if (existing != null) {
        if (!existing.requestHash().equals(hash)) throw new IdempotencyConflictException()
        return existing.toOrderResult()
    }
    validated = orderValidator.validateAndPrice(tenantId, command)
    order = orderDao.insert(validated.order())
    itemDao.batchInsert(order.id(), validated.items())
    idempotencyDao.insert(tenantId, command.key(), hash, order.result())
    return order.result()
}
```

- Verification contribution: makes sequential tests GREEN; transaction integration observes race/rollback.
- After this file: focused unit behavior is GREEN; database uniqueness race still requires File 3 integration coverage.

- Validation working directory: repository module root
- Verification command: `mvn -pl order-module -Dtest=OrderServiceImplTest,OrderIdempotencyIT test`
- Expected result: named tests pass; exactly one order/result exists for sequential and concurrent duplicates.
- Failure returns to: File 1 for assertion/fixture mismatch, File 2 for orchestration failure, or the migration Step for constraint/race failure.
- Completion criteria: both requirements have unit/integration evidence and no public route or unrelated class was added.
- Rollback: revert only the three Step paths; additive migration rollback follows the approved forward-fix boundary.
- Commit paths: exact test, implementation, and integration-test paths declared above.
- Commit: `feat(order): make creation idempotent`

## Frontend worked Step

Use the same file fields. A typical order is:

1. component/hook test naming loading, success, validation, forbidden, retry, and cache assertions;
2. exact API response/request type when approved and not already shared;
3. client/query hook with route/query key, request mapping, error mapping, and invalidation;
4. component/page render branches and events;
5. route/menu/export wiring only when not already registered.

Pseudocode should show, for example:

```typescript
const mutation = useMutation({
  mutationFn: (form: OrderFormState) => orderClient.create(mapFormToRequest(form)),
  onSuccess: ({data}) => {
    queryClient.invalidateQueries({queryKey: orderKeys.list()});
    navigate(`/orders/${data.orderId}`);
  },
  onError: (error) => setFieldErrors(mapValidationErrors(error)),
});

if (query.isPending) return <OrderSkeleton />;
if (query.isError) return <RetryPanel onRetry={query.refetch} />;
if (query.data.items.length === 0) return <EmptyOrders />;
return <OrderTable rows={query.data.items} />;
```

Name where tenant/identity comes from. Do not plan a parameter-fetch query when the target API derives it or the page already holds the selected resource. State the exact test command, typecheck/build gate, expected result, and manual-only boundary.

## Database migration worked Step

The file order normally includes:

1. migration/schema contract test asserting the missing column/constraint/index or current failure;
2. exactly one new next-version Flyway file;
3. PO/Entity/mapper changes that tolerate the approved compatibility window;
4. DAO/repository query and affected-row/lock behavior;
5. integration test for migration, historical rows, constraints, and query behavior.

SQL pseudocode must name execution order and guards:

```sql
-- profile/guard duplicate keys according to approved policy
ALTER TABLE orders ADD COLUMN idempotency_key varchar(64);
-- backfill only when Spec defines a deterministic source; otherwise preserve nullable compatibility
CREATE UNIQUE INDEX ... ON orders(tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
-- enforce NOT NULL only in the approved phase after old writers are removed
```

State dialect, exact migration path/version, lock/build risk, old/new application matrix, batch/restart owner, verification queries and expected counts, application rollback limit, and forward-fix. Never plan edits to an existing migration.

## Validation ladder

Every command row must include:

| Order | Working directory | Exact command/method | Scope | Expected result | Failure returns to | Runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| 1 RED | `<cwd>` | `<command>` | `<test>` | Fails for exact missing behavior | File N | Static/module |
| 2 GREEN | `<cwd>` | `<command>` | `<test/module>` | Exit 0; named tests pass | Owning file | Static/module |
| 3 Regression | `<cwd>` | `<command>` | `<module/cross-module>` | Exit 0; no repository-policy warnings | Owning Step | Static/module |
| 4 Manual/runtime | `<cwd/system>` | `<steps>` | `<live behavior>` | `<observable result>` | `<Step/follow-up>` | User-controlled |

Do not write `mvn test`, `npm test`, or “run build” without the repository root/module, selectors, required environment variables, expected test/result, timeout/runtime dependency, and interpretation of skipped tests or warnings.

Validation commands in a Plan are future instructions, not evidence that validation already passed.

## Commit and handoff contract

For every Step:

- list exact `Commit paths`; they must equal the Step's declared write scope;
- use one semantic message matching the observable outcome;
- state the baseline and end state so the next Step knows which contracts/tests exist;
- keep unrelated dirty/staged/untracked paths out of staging and commit;
- define rollback as a path-limited source revert, application rollback boundary, or forward fix according to data/compatibility risk;
- do not use one commit for several independent outcomes or empty commits for already-complete work.

If the same file must be modified in more than one Step, name the exact symbols/sections owned by each Step, explain why one atomic Step is worse, and ensure execution will not require committing a knowingly incomplete public contract.

## Final detail gate

Return `REVISE` when any applies:

- a Step or file description could be pasted into an unrelated repository unchanged;
- an implementer must choose a signature, field source, branch, error, transaction, query, page state, test assertion, or file order;
- pseudocode is shorter than the behavior it claims to define or uses generic verbs without real symbols;
- current repository evidence, dependencies/consumers, mapping, errors, verification contribution, or after-file state is absent;
- validation lacks a working directory, exact command/method, expected result, and failure return point;
- commit paths are not exact or include files outside the Step;
- a Step implements a Spec element that fails the simplicity/necessity audit;
- the Plan is detailed only at phase level rather than file and symbol level.
