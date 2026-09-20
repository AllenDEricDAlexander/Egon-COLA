package top.egon.cola.component.accessguard.policy;

import top.egon.cola.component.accessguard.core.failure.FailurePoint;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.List;

public enum GuardPolicyType implements EgonEnum {
    DENY_LIST(0, "deny-list", FailurePoint.DENY_LIST_STORE),
    ALLOW_LIST(1, "allow-list", FailurePoint.ALLOW_LIST_STORE),
    PENALTY_BOX(2, "penalty-box", FailurePoint.PENALTY_STORE),
    RATE_LIMIT(3, "rate-limit", FailurePoint.RATE_LIMIT_BACKEND);

    private static final List<GuardPolicyType> CANONICAL_ORDER =
            List.of(DENY_LIST, ALLOW_LIST, PENALTY_BOX, RATE_LIMIT);

    private final int code;

    private final String id;

    private final FailurePoint failurePoint;

    GuardPolicyType(int code, String id, FailurePoint failurePoint) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("policy id must not be blank");
        }
        this.code = code;
        this.id = id;
        this.failurePoint = java.util.Objects.requireNonNull(failurePoint, "failurePoint");
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return id;
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
