package top.egon.cola.archetype.source.lightopen.adapter.filter;

public record RequestContext(String operatorId, String requestId, String traceId, Long tenantId) {
    public RequestContext(String operatorId, String requestId, String traceId) {
        this(operatorId, requestId, traceId, null);
    }

    public static RequestContext anonymous(String traceId) {
        return new RequestContext("anonymous", traceId, traceId);
    }
}
