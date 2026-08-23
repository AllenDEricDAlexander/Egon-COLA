package top.egon.cola.component.accessguard.policy;

import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;

public interface GuardPolicy {

    GuardPolicyType type();

    PolicyResult evaluate(GuardContext context, AdmissionConfig admission);
}
