package top.egon.cola.component.accessguard.observability;

import top.egon.cola.component.accessguard.core.plan.GuardPlanChangedEvent;

public interface GuardEventPublisher {

    void publish(GuardEvent event);

    void publishStage(GuardStageEvent event);

    void publishPlanChanged(GuardPlanChangedEvent event);

    static GuardEventPublisher noop() {
        return NoopGuardEventPublisher.INSTANCE;
    }

    enum NoopGuardEventPublisher implements GuardEventPublisher {
        INSTANCE;

        @Override
        public void publish(GuardEvent event) {
        }

        @Override
        public void publishStage(GuardStageEvent event) {
        }

        @Override
        public void publishPlanChanged(GuardPlanChangedEvent event) {
        }
    }
}
