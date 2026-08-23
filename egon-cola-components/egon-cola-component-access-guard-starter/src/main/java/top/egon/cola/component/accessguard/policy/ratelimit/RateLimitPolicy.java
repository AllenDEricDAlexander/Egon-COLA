package top.egon.cola.component.accessguard.policy.ratelimit;

import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.policy.GuardContext;
import top.egon.cola.component.accessguard.policy.GuardPolicy;
import top.egon.cola.component.accessguard.policy.GuardPolicyType;
import top.egon.cola.component.accessguard.policy.PolicyResult;
import top.egon.cola.component.accessguard.store.RateLimitBackend;
import top.egon.cola.component.accessguard.store.RateLimitDecision;
import top.egon.cola.component.accessguard.store.RateLimitRequest;

public final class RateLimitPolicy implements GuardPolicy {

    private final RateLimitBackend backend;

    public RateLimitPolicy(RateLimitBackend backend) {
        this.backend = backend;
    }

    @Override
    public GuardPolicyType type() {
        return GuardPolicyType.RATE_LIMIT;
    }

    @Override
    public PolicyResult evaluate(GuardContext context, AdmissionConfig admission) {
        AdmissionConfig.RateLimitConfig config = admission.rateLimit();
        if (!config.enabled()) {
            return PolicyResult.pass();
        }
        RateLimitDecision decision = backend.acquire(new RateLimitRequest(
                context.ruleId(),
                context.stateVersion(),
                context.keyHash(),
                config.algorithm(),
                config.capacity(),
                config.refillTokens(),
                config.refillPeriod(),
                config.requestedTokens()));
        return decision.allowed()
                ? new PolicyResult(true, GuardDecision.PASS, java.util.Set.of(),
                        java.time.Duration.ZERO, decision.remainingTokens())
                : PolicyResult.reject(
                        GuardDecision.RATE_LIMITED,
                        decision.retryAfter(),
                        decision.remainingTokens());
    }
}
