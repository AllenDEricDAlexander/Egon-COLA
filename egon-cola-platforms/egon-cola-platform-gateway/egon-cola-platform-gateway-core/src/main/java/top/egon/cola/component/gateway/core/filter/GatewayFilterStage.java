package top.egon.cola.component.gateway.core.filter;

public enum GatewayFilterStage {

    REQUEST_IDENTITY(100),
    ACCESS_ZONE(200),
    REQUEST_GUARD(300),
    ROUTE_MATCH(400),
    EXPOSURE(500),
    CORS(600),
    AUTHENTICATION(700),
    AUTHORIZATION(800),
    RATE_CONCURRENCY(900),
    BINDING(1000),
    PROVIDER_SELECTION(1100),
    INVOCATION(1200),
    RESPONSE_MAPPING(1300),
    OBSERVATION(1400);

    private final int order;

    GatewayFilterStage(int order) {
        this.order = order;
    }

    public int order() {
        return order;
    }
}
