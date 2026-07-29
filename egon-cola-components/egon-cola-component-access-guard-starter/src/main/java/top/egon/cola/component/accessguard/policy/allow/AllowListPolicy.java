package top.egon.cola.component.accessguard.policy.allow;

import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.policy.GuardContext;
import top.egon.cola.component.accessguard.policy.GuardPolicy;
import top.egon.cola.component.accessguard.policy.PolicyResult;
import top.egon.cola.component.accessguard.store.AllowListStore;

import java.util.Set;

public final class AllowListPolicy implements GuardPolicy<AdmissionConfig.AllowListConfig> {

    private final AllowListStore store;

    public AllowListPolicy(AllowListStore store) {
        this.store = store;
    }

    @Override
    public String id() {
        return "allow-list";
    }

    @Override
    public PolicyResult evaluate(GuardContext context, AdmissionConfig.AllowListConfig config) {
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
        Set<String> bypassed = config.mode() == AllowListMode.BYPASS_RATE_LIMIT
                ? Set.of("rate-limit")
                : Set.of("penalty-box", "rate-limit");
        return PolicyResult.passWithBypass(bypassed);
    }
}
