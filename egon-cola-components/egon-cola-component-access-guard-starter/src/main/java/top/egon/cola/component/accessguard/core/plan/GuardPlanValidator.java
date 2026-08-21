package top.egon.cola.component.accessguard.core.plan;

import top.egon.cola.component.accessguard.core.failure.FailurePoint;
import top.egon.cola.component.accessguard.execution.RejectionMode;
import top.egon.cola.component.accessguard.execution.FallbackMethodCache;
import top.egon.cola.component.accessguard.execution.JsonRejectValueParser;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;
import top.egon.cola.component.accessguard.execution.TimeLimiterType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Objects;

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
                || rate.requestedTokens() > rate.capacity()
                || rate.refillPeriod().isZero() || rate.refillPeriod().isNegative()) {
            throw new IllegalArgumentException("rate-limit values must be positive");
        }
        if (rate.algorithm() == AdmissionConfig.RateLimitAlgorithm.SLIDING_WINDOW
                && rate.requestedTokens() != 1) {
            throw new IllegalArgumentException(
                    "SLIDING_WINDOW requires requestedTokens=1");
        }
        if (rate.algorithm() == AdmissionConfig.RateLimitAlgorithm.SLIDING_WINDOW
                && rate.capacity() > 100_000) {
            throw new IllegalArgumentException(
                    "SLIDING_WINDOW capacity must be <= 100000");
        }
        ExecutionConfig.TimeLimitConfig time = plan.execution().timeLimit();
        if (time.timeout().isZero() || time.timeout().isNegative()) {
            throw new IllegalArgumentException("time-limit timeout must be positive");
        }
        if (time.enabled()) {
            if (time.mode() == TimeLimitMode.DISABLED) {
                throw new IllegalArgumentException("enabled time-limit must declare a mode");
            }
            if (time.executor() == TimeLimiterType.CALLER_THREAD && time.mode() != TimeLimitMode.OBSERVE_ONLY) {
                throw new IllegalArgumentException("CALLER_THREAD is valid only for OBSERVE_ONLY");
            }
            if (time.executor() != TimeLimiterType.CALLER_THREAD && time.mode() != TimeLimitMode.ENFORCE) {
                throw new IllegalArgumentException("managed executors require ENFORCE mode");
            }
        }
        ExecutionConfig.RejectionConfig rejection = plan.execution().rejection();
        if (rejection.mode() == RejectionMode.FALLBACK && rejection.fallbackMethod().isBlank()) {
            throw new IllegalArgumentException("FALLBACK requires fallbackMethod");
        }
        if (rejection.mode() == RejectionMode.RETURN_JSON && rejection.returnJson().isBlank()) {
            throw new IllegalArgumentException("RETURN_JSON requires returnJson");
        }
        for (FailurePoint point : FailurePoint.values()) {
            plan.failurePolicies().policyFor(point);
        }
    }

    public void validateExecution(
            Executable executable,
            GuardPlan plan,
            FallbackMethodCache fallbackCache,
            JsonRejectValueParser jsonParser
    ) {
        Objects.requireNonNull(executable, "executable");
        Objects.requireNonNull(plan, "plan");
        ExecutionConfig execution = plan.execution();
        if (executable instanceof Constructor<?>) {
            if (execution.timeLimit().enabled() || execution.rejection().mode() != RejectionMode.THROW) {
                throw new IllegalArgumentException("constructors support admission and THROW rejection only");
            }
            return;
        }
        Method method = (Method) executable;
        ExecutionConfig.RejectionConfig rejection = execution.rejection();
        if (rejection.mode() == RejectionMode.FALLBACK) {
            Objects.requireNonNull(fallbackCache, "fallbackCache")
                    .validateAndCache(method, rejection.fallbackMethod());
        } else if (rejection.mode() == RejectionMode.RETURN_JSON) {
            Objects.requireNonNull(jsonParser, "jsonParser")
                    .parse(rejection.returnJson(), method.getReturnType());
        } else if (rejection.mode() == RejectionMode.RETURN_NULL && method.getReturnType().isPrimitive()) {
            throw new IllegalArgumentException("primitive return types do not support RETURN_NULL");
        }
    }
}
