# Gateway RPC Controller Test Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Repair the stale Gateway RPC controller test, complete repository verification, merge the logging consolidation into `main`, and remove its temporary branch and worktree.

**Architecture:** Keep the production W3C Trace Context contract unchanged. Update the unit test to call the existing three-argument controller entry point with a valid `traceparent` and request ID, then verify the trace ID visible to the RPC client and the scope cleanup after the call.

**Tech Stack:** Java 21, Spring Boot, JUnit Jupiter, AssertJ, Maven, Git.

## Global Constraints

- Change only the stale Gateway RPC controller test and this plan before merging.
- Do not add a legacy controller overload or weaken W3C Trace Context validation.
- Run the full repository `clean integration-test` before and after merging.
- Do not start applications or services.
- Delete only the task-owned worktree and feature branch after merged verification passes.

---

### Task 1: Align the controller test with W3C Trace Context

**Files:**
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-rpc-consumer/src/test/java/top/egon/cola/component/gateway/test/rpc/consumer/GatewayRpcDriverControllerTest.java`
- Create: `docs/superpowers/plans/2026-07-29-gateway-rpc-controller-test-repair.md`

**Interfaces:**
- Consumes: `GatewayRpcDriverController.echo(String message, String traceparent, String requestId)`.
- Produces: a regression test proving that a valid inbound `traceparent` supplies the RPC call's trace ID and that `TraceContext` is restored afterward.

- [ ] **Step 1: Reproduce the stale-call failure**

Run:

```bash
mvn -f egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-rpc-consumer/pom.xml \
  -Dtest=GatewayRpcDriverControllerTest test
```

Expected: test compilation fails because the test supplies two arguments to the three-argument `echo` method.

- [ ] **Step 2: Update the test input and expected trace ID**

Call the controller with these literal values:

```java
GatewayRpcDriverController.EchoView response = controller.echo(
        "through-gateway",
        "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
        "rpc-driver-request"
);
```

Assert that `response.traceId()` equals `4bf92f3577b34da6a3ce929d0e0e4736` and that `TraceContext.getTraceId()` is null after the call.

- [ ] **Step 3: Verify the focused test**

Run from the repository root so the current reactor sources supply all changed
dependencies instead of resolving an older locally installed snapshot:

```bash
mvn -pl :egon-cola-component-gateway-test-rpc-consumer -am \
  -Dtest=GatewayRpcDriverControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: one target test passes with zero failures and errors, and every
required reactor module succeeds.

- [ ] **Step 4: Verify the complete feature branch**

Run:

```bash
mvn clean integration-test
```

Expected: the complete reactor exits zero.

- [ ] **Step 5: Commit the repair**

```bash
git add docs/superpowers/plans/2026-07-29-gateway-rpc-controller-test-repair.md \
  egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-rpc-consumer/src/test/java/top/egon/cola/component/gateway/test/rpc/consumer/GatewayRpcDriverControllerTest.java
git commit -m "test(gateway): align rpc driver trace contract"
```

### Task 2: Merge and clean up

**Files:**
- No additional source changes.

**Interfaces:**
- Consumes: feature branch `codex/common-log-util-consolidation` based on `main`.
- Produces: verified `main`, with the temporary worktree and feature branch removed.

- [ ] **Step 1: Merge into main**

From the main worktree, fast-forward `main` to the feature branch after confirming both worktrees are clean.

- [ ] **Step 2: Verify the merged result**

Run `mvn clean integration-test` from the main worktree. Expected: the complete reactor exits zero.

- [ ] **Step 3: Remove task-owned Git state**

Remove `.worktrees/common-log-util-consolidation`, prune worktree metadata, and delete `codex/common-log-util-consolidation` with a normal non-force branch deletion.

- [ ] **Step 4: Audit the final state**

Confirm `main` is clean, the feature branch no longer exists, and the task-owned worktree is no longer registered.
