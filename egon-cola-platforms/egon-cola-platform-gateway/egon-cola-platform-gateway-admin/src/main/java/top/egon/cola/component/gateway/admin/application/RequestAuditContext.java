package top.egon.cola.component.gateway.admin.application;

import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.common.trace.TraceState;

public record RequestAuditContext(String requestId, String traceId) {

    public static RequestAuditContext current() {
        return current(null);
    }

    public static RequestAuditContext current(String requestId) {
        TraceState state = TraceContext.currentOrCreate();
        String resolvedRequestId = safe(requestId)
                ? requestId.trim()
                : state.requestId() == null
                ? state.traceId()
                : state.requestId();
        return new RequestAuditContext(resolvedRequestId, state.traceId());
    }

    private static boolean safe(String value) {
        return value != null
                && !value.isBlank()
                && value.indexOf('\r') < 0
                && value.indexOf('\n') < 0;
    }
}
