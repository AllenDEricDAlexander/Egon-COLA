package top.egon.cola.component.accessguard.policy.allow;

import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.policy.GuardContext;
import top.egon.cola.component.accessguard.policy.GuardPolicy;
import top.egon.cola.component.accessguard.policy.GuardPolicyType;
import top.egon.cola.component.accessguard.policy.PolicyResult;
import top.egon.cola.component.accessguard.store.AllowListStore;

import java.util.Set;

public final class AllowListPolicy implements GuardPolicy {

    private final AllowListStore store;

    public AllowListPolicy(AllowListStore store) {
        this.store = store;
    }

    @Override
    public GuardPolicyType type() {
        return GuardPolicyType.ALLOW_LIST;
    }

    @Override
    public PolicyResult evaluate(GuardContext context, AdmissionConfig admission) {
        AdmissionConfig.AllowListConfig config = admission.allowList();
        if (!config.enabled()) {
            return PolicyResult.pass();
        }
        boolean member = store.contains(context.ruleId(), config.dataVersion(), context.keyHash());
        if (config.mode() == AllowListMode.GATE) {
            return member ? PolicyResult.pass() : PolicyResult.reject(GuardDecision.ALLOW_LIST_MISS);
        }
        if (!member) {
            return PolicyResult.pass();
        }
        Set<GuardPolicyType> bypassed = config.mode() == AllowListMode.BYPASS_RATE_LIMIT
                ? Set.of(GuardPolicyType.RATE_LIMIT)
                : Set.of(GuardPolicyType.PENALTY_BOX, GuardPolicyType.RATE_LIMIT);
        return PolicyResult.passWithBypass(bypassed);
    }
}
