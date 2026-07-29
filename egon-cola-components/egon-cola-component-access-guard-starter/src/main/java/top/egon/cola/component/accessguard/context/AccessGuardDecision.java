package top.egon.cola.component.accessguard.context;

public enum AccessGuardDecision {
    PASS,
    WHITELIST_REJECTED,
    RATE_LIMITED,
    BLACKLIST_HIT,
    CIRCUIT_BREAKER_TIMEOUT,
    CIRCUIT_BREAKER_REJECTED,
    REDISSON_ERROR,
    KEY_RESOLVE_FAILED,
    FALLBACK_INVOKED,
    RETURN_JSON_INVOKED
}
