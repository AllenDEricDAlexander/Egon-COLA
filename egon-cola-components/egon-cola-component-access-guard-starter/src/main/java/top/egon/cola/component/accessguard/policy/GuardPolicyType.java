package top.egon.cola.component.accessguard.policy;

import top.egon.cola.component.accessguard.core.failure.FailurePoint;

import java.util.List;

public enum GuardPolicyType {
    DENY_LIST("deny-list", FailurePoint.DENY_LIST_STORE),
    ALLOW_LIST("allow-list", FailurePoint.ALLOW_LIST_STORE),
    PENALTY_BOX("penalty-box", FailurePoint.PENALTY_STORE),
    RATE_LIMIT("rate-limit", FailurePoint.RATE_LIMIT_BACKEND);

    private static final List<GuardPolicyType> CANONICAL_ORDER =
            List.of(DENY_LIST, ALLOW_LIST, PENALTY_BOX, RATE_LIMIT);

    private final String id;
    private final FailurePoint failurePoint;

    GuardPolicyType(String id, FailurePoint failurePoint) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("policy id must not be blank");
        }
        this.id = id;
        this.failurePoint = java.util.Objects.requireNonNull(failurePoint, "failurePoint");
    }

    public String id() {
        return id;
    }

    public FailurePoint failurePoint() {
        return failurePoint;
    }

    public static List<GuardPolicyType> canonicalOrder() {
        return CANONICAL_ORDER;
    }
}
