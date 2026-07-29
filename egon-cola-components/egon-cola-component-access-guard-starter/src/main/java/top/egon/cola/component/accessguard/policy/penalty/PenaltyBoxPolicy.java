package top.egon.cola.component.accessguard.policy.penalty;

import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.policy.GuardContext;
import top.egon.cola.component.accessguard.policy.GuardPolicy;
import top.egon.cola.component.accessguard.policy.PolicyResult;
import top.egon.cola.component.accessguard.store.PenaltyKey;
import top.egon.cola.component.accessguard.store.PenaltyStore;

public final class PenaltyBoxPolicy implements GuardPolicy<AdmissionConfig.PenaltyBoxConfig> {

    private final PenaltyStore store;

    public PenaltyBoxPolicy(PenaltyStore store) {
        this.store = store;
    }

    @Override
    public String id() {
        return "penalty-box";
    }

    @Override
    public PolicyResult evaluate(GuardContext context, AdmissionConfig.PenaltyBoxConfig config) {
        if (!config.enabled()) {
            return PolicyResult.pass();
        }
        return store.current(new PenaltyKey(context.ruleId(), context.stateVersion(), context.keyHash()))
                .filter(state -> state.active())
                .map(ignored -> PolicyResult.reject(GuardDecision.PENALTY_ACTIVE))
                .orElseGet(PolicyResult::pass);
    }
}
