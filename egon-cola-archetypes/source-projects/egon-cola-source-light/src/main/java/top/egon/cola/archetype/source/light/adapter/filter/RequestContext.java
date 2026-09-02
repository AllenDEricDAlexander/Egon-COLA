package top.egon.cola.archetype.source.light.adapter.filter;

public record RequestContext(String operatorId, String requestId, String traceId) {
    public static RequestContext anonymous(String traceId) {
        return new RequestContext("anonymous", traceId, traceId);
    }
}
