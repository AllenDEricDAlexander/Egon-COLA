package top.egon.cola.component.accessguard.policy;

import top.egon.cola.component.accessguard.policy.allow.AllowListPolicy;
import top.egon.cola.component.accessguard.policy.deny.DenyListPolicy;
import top.egon.cola.component.accessguard.policy.penalty.PenaltyBoxPolicy;
import top.egon.cola.component.accessguard.policy.ratelimit.RateLimitPolicy;

import java.util.List;

public final class AdmissionPolicies {

    private AdmissionPolicies() {
    }

    public static List<GuardPolicy<?>> builtIns(
            DenyListPolicy denyList,
            AllowListPolicy allowList,
            PenaltyBoxPolicy penaltyBox,
            RateLimitPolicy rateLimit
    ) {
        return List.of(denyList, allowList, penaltyBox, rateLimit);
    }
}
