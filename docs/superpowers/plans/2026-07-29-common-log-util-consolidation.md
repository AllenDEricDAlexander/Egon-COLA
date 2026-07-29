# CommonLogUtil Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the standalone common-log artifact with one MDC-aware `CommonLogUtil` in common-trace.

**Architecture:** `CommonLogUtil` is the only top-level logging API and owns a nested fluent builder plus the single-line rendering algorithm. It reads the current MDC only when a terminal log method is called, flattens MDC and business fields into the emitted message, and leaves Trace context ownership in the existing `TraceContext`/`TraceScope` types.

**Tech Stack:** Java 21, SLF4J 2.0.18, Logback 1.5.34 for tests, JUnit Jupiter 5.12.2, Maven.

## Global Constraints

- Expose `top.egon.cola.component.common.trace.CommonLogUtil`; do not name it `TraceLogUtil`.
- Keep exactly one top-level production logging class.
- Delete `egon-cola-component-common-log`; do not add a compatibility artifact.
- Include the complete current MDC content in the emitted business-log message.
- Keep production dependencies limited to the existing `slf4j-api` dependency.
- Preserve existing Trace context and propagation behavior.
- Do not start any application or service.

---

### Task 1: Consolidate business logging into common-trace

**Files:**
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace/src/main/java/top/egon/cola/component/common/trace/CommonLogUtil.java`
- Create: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace/src/test/java/top/egon/cola/component/common/trace/CommonLogUtilTest.java`
- Delete: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-log/`
- Modify: `egon-cola-components/egon-cola-component-common/pom.xml`
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`
- Modify: `egon-cola-components/egon-cola-component-common/README.md`
- Modify: `egon-cola-components/egon-cola-component-common/README.zh-CN.md`
- Modify: `egon-cola-components/egon-cola-components-bom/README.md`
- Modify: `egon-cola-components/egon-cola-components-bom/README.zh-CN.md`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace-spring-boot-starter/README.md`
- Modify: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-trace-spring-boot-starter/README.zh-CN.md`
- Modify: `docs/superpowers/specs/2026-07-28-trace-context-log-correlation-design.md`

**Interfaces:**
- Consumes: `org.slf4j.Logger`, `org.slf4j.MDC`, and the existing `TraceKeys.ownedMdcKeys()` contract.
- Produces: `CommonLogUtil.bizDebug(Logger)`, `bizInfo(Logger)`, `bizWarn(Logger)`, and `bizError(Logger)`, each returning `CommonLogUtil.BizLogBuilder`.
- Produces: nested `Phase` and `BizLogBuilder` types with the fluent and terminal methods listed in the design.

- [ ] **Step 1: Write the failing behavior tests**

Create `CommonLogUtilTest` with a real `ListAppender<ILoggingEvent>`. Use a
fixed `TraceState`, add `tenantId` directly to MDC, and assert the literal
formatted message:

```java
assertEquals(
        "traceId=0123456789abcdef0123456789abcdef "
                + "spanId=0123456789abcdef requestId=request-1 "
                + "traceFlags=01 "
                + "tenantId=tenant-1 biz=order scene=create "
                + "result=SUCCESS msg=\"order created\"",
        onlyEvent().getFormattedMessage()
);
```

Add separate tests that prove a throwable is preserved, sensitive extension
fields are masked, line breaks are removed, collections stop after 20 entries,
and a disabled level emits no event.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
mvn -f egon-cola-components/egon-cola-component-common/pom.xml \
  -pl egon-cola-component-common-trace \
  -Dtest=CommonLogUtilTest test
```

Expected: test compilation fails because `CommonLogUtil` does not exist.

- [ ] **Step 3: Add the minimal one-class implementation**

Create the public entry points and nested types:

```java
public final class CommonLogUtil {
    public static BizLogBuilder bizDebug(Logger logger) { ... }
    public static BizLogBuilder bizInfo(Logger logger) { ... }
    public static BizLogBuilder bizWarn(Logger logger) { ... }
    public static BizLogBuilder bizError(Logger logger) { ... }

    public enum Phase { START, LOAD, CHECK, DECISION, CREATE, UPDATE,
        DELETE, CALL, SEND, PROCESS, END }

    public static final class BizLogBuilder {
        // fluent business fields and terminal log methods
    }
}
```

Before rendering, combine fields in this order:

```java
Map<String, String> mdc = MDC.getCopyOfContextMap();
for (String key : MDC_FIELD_ORDER) {
    append the present MDC value;
}
append remaining MDC entries sorted by key;
append business fields in BIZ_LOG_FIELD_ORDER;
append remaining business fields in insertion order;
```

Pass the final string to the corresponding `Logger.debug/info/warn/error`
method and pass the original throwable to warn/error when present.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the Step 2 command again. Expected: all `CommonLogUtilTest` tests pass with
zero failures and zero errors.

- [ ] **Step 5: Remove the old artifact and synchronize repository contracts**

Delete the complete `egon-cola-component-common-log` directory. Remove its
`<module>` entry from the common aggregator and its dependency-management entry
from the components BOM. Replace README dependency and API examples with:

```java
CommonLogUtil.bizInfo(LOG)
        .biz("order")
        .scene("create")
        .success("order created");
```

Correct the existing Trace design document so it states that business logging
is part of `common-trace`, uses one `CommonLogUtil`, and renders MDC into the
message instead of relying on a separate common-log artifact or `%kvp`.

- [ ] **Step 6: Run targeted and reactor verification**

Run:

```bash
mvn -f egon-cola-components/egon-cola-component-common/pom.xml test
mvn clean integration-test
```

Expected: both commands exit 0. The root command validates downstream module
and archetype dependency graphs after removal of the old artifact.

- [ ] **Step 7: Review and commit the implementation**

Run:

```bash
git diff --check
rg -n -g '!target' 'egon-cola-component-common-log|common\.log|BizLogFields' .
git status --short
```

The `rg` result may contain only historical text explicitly describing the
superseded design; current POMs, READMEs, and source code must contain no stale
artifact or package reference. Then commit the scoped change:

```bash
git add docs/superpowers \
  egon-cola-components/egon-cola-component-common \
  egon-cola-components/egon-cola-components-bom
git commit -m "refactor(trace): consolidate common logging utility"
```
