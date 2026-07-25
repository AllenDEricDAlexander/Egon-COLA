package top.egon.cola.component.gateway.engine.traffic;

public enum TrafficPolicyType {

    RATE_LIMIT,
    TIMEOUT,
    BULKHEAD,
    CIRCUIT_BREAKER,
    RETRY,
    REQUEST_SIZE,
    RESPONSE_SIZE
}
