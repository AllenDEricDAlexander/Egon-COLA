package top.egon.cola.component.accessguard.policy.penalty;

import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.policy.GuardContext;
import top.egon.cola.component.accessguard.store.PenaltyState;

public interface PenaltyService {

    PenaltyState recordViolation(GuardContext context, AdmissionConfig.PenaltyBoxConfig config);
}
