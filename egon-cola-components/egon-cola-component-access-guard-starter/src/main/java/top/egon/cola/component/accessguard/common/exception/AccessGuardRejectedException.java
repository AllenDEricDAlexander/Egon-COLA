package top.egon.cola.component.accessguard.common.exception;

import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;

import java.io.Serial;
import java.util.Objects;

/**
 * Thrown when a guard rejects a call and the resolved resolution is a throw.
 *
 * <p>The numeric code classifies the failure family while {@code ACCESS_GUARD_REJECTED} stays the
 * stable detail carried by {@link #getStatus()}; the full {@link GuardOutcome} remains available for
 * protocol-aware consumers.</p>
 */
public final class AccessGuardRejectedException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String CODE = "ACCESS_GUARD_REJECTED";

    private final GuardOutcome outcome;

    public AccessGuardRejectedException(GuardOutcome outcome) {
        super(ResultCode.FORBIDDEN.getCode(), CODE, message(Objects.requireNonNull(outcome, "outcome")));
        this.outcome = outcome;
    }

    /** @return the retained rejection outcome */
    public GuardOutcome outcome() {
        return outcome;
    }

    private static String message(GuardOutcome outcome) {
        return "Access Guard rejected rule=" + outcome.ruleId()
                + ", decision=" + outcome.decision()
                + ", resolution=" + outcome.resolution();
    }
}
