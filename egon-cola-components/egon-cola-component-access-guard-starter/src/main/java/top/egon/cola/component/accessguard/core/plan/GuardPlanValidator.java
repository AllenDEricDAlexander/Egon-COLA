package top.egon.cola.component.accessguard.core.plan;

import top.egon.cola.component.accessguard.core.failure.FailurePoint;

public final class GuardPlanValidator {

    public void validate(GuardPlanSnapshot snapshot) {
        GuardPlan plan = snapshot.plan();
        if (!snapshot.ruleId().equals(plan.id())) {
            throw new IllegalArgumentException("snapshot ruleId must match plan id");
        }
        AdmissionConfig.PenaltyBoxConfig penalty = plan.admission().penaltyBox();
        if (penalty.threshold() <= 0 || penalty.violationTtl().isZero() || penalty.violationTtl().isNegative()
                || penalty.penaltyTtl().isZero() || penalty.penaltyTtl().isNegative()) {
            throw new IllegalArgumentException("penalty values must be positive");
        }
        AdmissionConfig.RateLimitConfig rate = plan.admission().rateLimit();
        if (rate.capacity() <= 0 || rate.refillTokens() <= 0 || rate.requestedTokens() <= 0
                || rate.refillPeriod().isZero() || rate.refillPeriod().isNegative()) {
            throw new IllegalArgumentException("rate-limit values must be positive");
        }
        ExecutionConfig.TimeLimitConfig time = plan.execution().timeLimit();
        if (time.timeout().isZero() || time.timeout().isNegative()) {
            throw new IllegalArgumentException("time-limit timeout must be positive");
        }
        for (FailurePoint point : FailurePoint.values()) {
            plan.failurePolicies().policyFor(point);
        }
    }
}
