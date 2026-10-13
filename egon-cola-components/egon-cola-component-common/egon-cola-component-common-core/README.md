# Common core exception handling

`CommonException` defaults to `SYSTEM`; expected `BusinessException` failures default to
`BUSINESS` and WARN. `BusinessException.alert(status, cause, details)` explicitly creates a
`BUSINESS_ALERT`. Built-in system/dependency codes and wrapping a classified alert also retain
alert semantics. A cause alone does not mark an expected rejection as an alert.

Use `ExceptionLogger.log(log, failure)` once at the boundary that owns handling the failure.
The logger is a small Facade over the existing SLF4J structured logging API; it has no Spring,
alert-provider or runtime registration dependency. Constructors and factories never log.
A resolver returning control to the next handler must leave logging to that owner.

Collect alerts by `alertRequired=true`, independently of the log level and `retryable` flag.
Events include `exceptionCategory`, code/status, retryability, exception class, severity,
trace ID and request ID. SYSTEM and BUSINESS_ALERT use ERROR (FATAL retains a separate marker).
An explicit level is preserved for ordinary BUSINESS failures, including an explicit ERROR
without an alert. Known asynchronous wrappers preserve their classified cause.

Diagnostic frames and bounded cause/suppressed structure are retained, while all free-form
exception messages are replaced by `[message redacted]`. No details or request payload is logged.
Original exceptions, their causes and HTTP/RPC response mappings remain unchanged. Existing
constructor signatures and serialVersionUID are retained; legacy deserialization infers the
family when the new category field is absent. Alert delivery is owned by the log collector.
