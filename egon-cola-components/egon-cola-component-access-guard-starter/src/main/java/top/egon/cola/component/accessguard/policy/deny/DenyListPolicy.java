package top.egon.cola.component.accessguard.policy.deny;

import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.policy.GuardContext;
import top.egon.cola.component.accessguard.policy.GuardPolicy;
import top.egon.cola.component.accessguard.policy.PolicyResult;
import top.egon.cola.component.accessguard.store.DenyListStore;

public final class DenyListPolicy implements GuardPolicy<AdmissionConfig.DenyListConfig> {

    private final DenyListStore store;

    public DenyListPolicy(DenyListStore store) {
        this.store = store;
    }

    @Override
    public String id() {
        return "deny-list";
    }

    @Override
    public PolicyResult evaluate(GuardContext context, AdmissionConfig.DenyListConfig config) {
        if (!config.enabled()) {
            return PolicyResult.pass();
        }
        return store.contains(context.ruleId(), config.dataVersion(), context.keyHash())
                ? PolicyResult.reject(GuardDecision.DENY_LIST_HIT)
                : PolicyResult.pass();
    }
}
