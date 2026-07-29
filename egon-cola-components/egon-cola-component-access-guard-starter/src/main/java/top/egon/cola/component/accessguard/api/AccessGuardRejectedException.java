package top.egon.cola.component.accessguard.api;

import top.egon.cola.component.accessguard.core.GuardOutcome;

import java.util.Objects;

public final class AccessGuardRejectedException extends RuntimeException {

    public static final String CODE = "ACCESS_GUARD_REJECTED";

    private final GuardOutcome outcome;

    public AccessGuardRejectedException(GuardOutcome outcome) {
        super(message(Objects.requireNonNull(outcome, "outcome")));
        this.outcome = outcome;
    }

    public String code() {
        return CODE;
    }

    public GuardOutcome outcome() {
        return outcome;
    }

    private static String message(GuardOutcome outcome) {
        return "Access Guard rejected rule=" + outcome.ruleId()
                + ", decision=" + outcome.decision()
                + ", resolution=" + outcome.resolution();
    }
}
