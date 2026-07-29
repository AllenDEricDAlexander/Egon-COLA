package top.egon.cola.component.accessguard.observability;

import top.egon.cola.component.accessguard.core.plan.GuardPlanChangedEvent;

@FunctionalInterface
public interface GuardEventListener {

    void onEvent(GuardEvent event);

    default void onStage(GuardStageEvent event) {
    }

    default void onPlanChanged(GuardPlanChangedEvent event) {
    }
}
