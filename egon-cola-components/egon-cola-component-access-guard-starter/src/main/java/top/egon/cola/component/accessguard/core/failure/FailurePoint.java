package top.egon.cola.component.accessguard.core.failure;

public enum FailurePoint {
    KEY_RESOLUTION,
    DENY_LIST_STORE,
    ALLOW_LIST_STORE,
    PENALTY_STORE,
    RATE_LIMIT_BACKEND,
    EXECUTION,
    OBSERVABILITY
}
