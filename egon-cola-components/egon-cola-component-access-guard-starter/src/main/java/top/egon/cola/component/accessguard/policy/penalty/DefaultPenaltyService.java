package top.egon.cola.component.accessguard.policy.penalty;

import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.policy.GuardContext;
import top.egon.cola.component.accessguard.store.PenaltyKey;
import top.egon.cola.component.accessguard.store.PenaltyState;
import top.egon.cola.component.accessguard.store.PenaltyStore;

public final class DefaultPenaltyService implements PenaltyService {

    private final PenaltyStore store;

    public DefaultPenaltyService(PenaltyStore store) {
        this.store = store;
    }

    @Override
    public PenaltyState recordViolation(GuardContext context, AdmissionConfig.PenaltyBoxConfig config) {
        return store.recordViolation(
                new PenaltyKey(context.ruleId(), context.stateVersion(), context.keyHash()),
                config.threshold(),
                config.violationTtl(),
                config.penaltyTtl());
    }
}
