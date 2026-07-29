package top.egon.cola.component.accessguard.core;

public enum GuardDecision {
    PASS,
    DENY_LIST_HIT,
    ALLOW_LIST_MISS,
    PENALTY_ACTIVE,
    RATE_LIMITED,
    KEY_RESOLUTION_FAILED,
    STORE_FAILED,
    CONFIG_FAILED,
    TIME_LIMIT_EXCEEDED,
    EXECUTOR_REJECTED,
    BUSINESS_EXCEPTION,
    CANCELLED
}
