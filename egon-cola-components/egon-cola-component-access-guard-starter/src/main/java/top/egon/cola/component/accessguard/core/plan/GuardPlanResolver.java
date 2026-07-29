package top.egon.cola.component.accessguard.core.plan;

import java.util.Optional;

public interface GuardPlanResolver {

    GuardPlanSnapshot resolve(String ruleId);

    default Optional<GuardPlanLoadFailure> lastFailure(String ruleId) {
        return Optional.empty();
    }
}
