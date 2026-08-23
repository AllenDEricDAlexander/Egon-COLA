package top.egon.cola.component.accessguard.policy.deny;

import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.policy.GuardContext;
import top.egon.cola.component.accessguard.policy.GuardPolicy;
import top.egon.cola.component.accessguard.policy.GuardPolicyType;
import top.egon.cola.component.accessguard.policy.PolicyResult;
import top.egon.cola.component.accessguard.store.DenyListStore;

public final class DenyListPolicy implements GuardPolicy {

    private final DenyListStore store;

    public DenyListPolicy(DenyListStore store) {
        this.store = store;
    }

    @Override
    public GuardPolicyType type() {
        return GuardPolicyType.DENY_LIST;
    }

    @Override
    public PolicyResult evaluate(GuardContext context, AdmissionConfig admission) {
        AdmissionConfig.DenyListConfig config = admission.denyList();
        if (!config.enabled()) {
            return PolicyResult.pass();
        }
        return store.contains(context.ruleId(), config.dataVersion(), context.keyHash())
                ? PolicyResult.reject(GuardDecision.DENY_LIST_HIT)
                : PolicyResult.pass();
    }
}
