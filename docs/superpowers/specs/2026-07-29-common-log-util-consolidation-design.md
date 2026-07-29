# CommonLogUtil Consolidation Design

## Context

The current common component exposes structured business logging through a
dedicated `egon-cola-component-common-log` artifact and three public top-level
classes: `BizLog`, `BizLogBuilder`, and `BizLogFields`. This boundary is larger
than the behavior requires, and it leaves Trace/MDC state and the log rendering
API in separate runtime artifacts.

The corrected design removes the standalone log artifact and exposes one
top-level logging utility from `egon-cola-component-common-trace`:
`top.egon.cola.component.common.trace.CommonLogUtil`.

## Scope

- Delete `egon-cola-component-common-log` completely.
- Remove the deleted artifact from the common reactor and components BOM.
- Add one public top-level class, `CommonLogUtil`, to `common-trace`.
- Keep the fluent business-log builder as a nested type of `CommonLogUtil`.
- Render the complete MDC snapshot into every business log message.
- Update current README and architecture documentation.
- Do not add Spring, Logback, Jackson, or a logging backend to production
  dependencies.

## Public API

`CommonLogUtil` provides the four business-log entry points:

```java
CommonLogUtil.bizDebug(Logger logger)
CommonLogUtil.bizInfo(Logger logger)
CommonLogUtil.bizWarn(Logger logger)
CommonLogUtil.bizError(Logger logger)
```

Each method returns the nested `CommonLogUtil.BizLogBuilder`. The builder keeps
the business-oriented methods demonstrated by the reference implementation:

- identity: `biz`, `scene`, `step`, `phase`, `bill`, `billIds`, `bizId`, `bizUk`
- state: `status`, `expectedStatus`, `statusChange`
- decision and outcome: `decision`, `reason`, `changed`, `errorCode`
- timing and message: `costMs`, `costSince`, `msg`
- controlled extension: `field`, `fields`
- terminal operations: `start`, `success`, `log`, `reject`, `warn`, `fail`

`Phase` and `BizLogBuilder` are nested types. No other top-level logging class
is introduced.

## Rendering Contract

At the terminal log call, `CommonLogUtil` captures
`MDC.getCopyOfContextMap()`. It renders Trace-owned MDC keys first in the order
defined by `TraceKeys`, then remaining MDC keys in lexical order, then business
fields in their stable business order. Explicit builder fields override an MDC
entry with the same key.

The final SLF4J message contains the flattened fields directly, for example:

```text
traceId=0123456789abcdef0123456789abcdef spanId=0123456789abcdef requestId=req-1 tenantId=tenant-1 biz=order scene=create result=SUCCESS msg="order created"
```

This contract does not depend on `%X`, `%mdc`, `%kvp`, a particular Logback
pattern, or SLF4J fluent key-value rendering. A logging backend may still add
its normal timestamp, level, logger name, and throwable stack.

Keys are normalized to letters, digits, `_`, `-`, and `.`. Values are converted
to a single line, collections/arrays/maps are bounded to 20 entries, and each
field is capped at 1,000 characters. Password, token, secret, authorization,
cookie, phone, bank-card, identity-card, and email-shaped fields receive the
same basic key-driven masking as the reference utility.

## Error and Level Behavior

- Disabled log levels return before MDC capture and message construction.
- Blank keys, null values, and blank string values are ignored.
- `fail(reason, throwable)` records `result=FAIL`, the reason, and the bounded
  exception message, while passing the original throwable to SLF4J.
- Logging never mutates or clears MDC.

## Design Pattern Decision

The nested Builder pattern is retained because it makes optional business
fields readable and prevents long positional argument lists. Separate Strategy,
Factory, or handler types are not warranted: level selection is a four-value
internal enum, rendering has one stable algorithm, and additional top-level
types would recreate the module shape being removed.

## Compatibility

The repository has no production caller of `BizLog`, `BizLogBuilder`, or
`BizLogFields`; only the current module tests and documentation reference them.
The old artifact and APIs are therefore removed directly without a deprecated
forwarder. External consumers must replace the dependency with
`egon-cola-component-common-trace` and call `CommonLogUtil`.

## Verification

`CommonLogUtilTest` uses a real Logback test appender. It verifies stable
rendering of Trace-owned and custom MDC values, business fields, throwable
preservation, masking, single-line normalization, bounded collections, and
disabled-level behavior. The final checks are the full common-module reactor
and the repository integration-test lifecycle; neither command starts a
service.
