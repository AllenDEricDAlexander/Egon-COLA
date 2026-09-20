package top.egon.cola.component.accessguard.observability;

import top.egon.cola.component.accessguard.core.plan.GuardPlanChangedEvent;
import top.egon.cola.component.common.core.enums.EgonEnum;

public interface GuardEventPublisher {

    void publish(GuardEvent event);

    void publishStage(GuardStageEvent event);

    void publishPlanChanged(GuardPlanChangedEvent event);

    static GuardEventPublisher noop() {
        return NoopGuardEventPublisher.INSTANCE;
    }

    enum NoopGuardEventPublisher implements GuardEventPublisher, EgonEnum {
        INSTANCE(0, "INSTANCE");

        private final int code;

        private final String message;

        NoopGuardEventPublisher(int code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }

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
