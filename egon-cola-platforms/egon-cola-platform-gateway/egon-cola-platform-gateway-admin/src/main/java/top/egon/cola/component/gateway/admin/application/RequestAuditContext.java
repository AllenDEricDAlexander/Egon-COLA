package top.egon.cola.component.gateway.admin.application;

import top.egon.cola.component.common.trace.TraceContext;

public record RequestAuditContext(String requestId, String traceId) {

    public static RequestAuditContext current() {
        return current(null);
    }

    public static RequestAuditContext current(String requestId) {
        TraceContext context = TraceContext.currentOrCreate();
        String resolvedRequestId = safe(requestId)
                ? requestId.trim()
                : context.requestId() == null
                ? context.traceId()
                : context.requestId();
        return new RequestAuditContext(resolvedRequestId, context.traceId());
    }

    private static boolean safe(String value) {
        return value != null
                && !value.isBlank()
                && value.indexOf('\r') < 0
                && value.indexOf('\n') < 0;
    }
}
