package top.egon.cola.component.tianshu.admin.service.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiPolicyStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiOptimizationStateTransition;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiOptimizationStateTransition.Context;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiOptimizationStateTransition.Signal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcAiOptimizationStateTransitionTest {

    private final DdcAiOptimizationStateTransition transition = new DdcAiOptimizationStateTransition();

    @ParameterizedTest
    @CsvSource({
            "ARMED,INSUFFICIENT_SAMPLES,false,0,ARMED",
            "ARMED,COOLING_DOWN,false,0,ARMED",
            "ARMED,NOT_PUBLISHED,false,0,ARMED",
            "ARMED,RUN_CREATED,false,0,ARMED",
            "ARMED,THRESHOLD_MET,false,0,CONVERGED",
            "ARMED,ITERATIONS_EXHAUSTED,false,0,EXHAUSTED",
            "ARMED,RUN_SUCCEEDED,false,0,AWAITING_PUBLISH",
            "ARMED,RUN_SUCCEEDED,true,0,ARMED",
            "ARMED,RUN_FAILED,false,2,ARMED",
            "ARMED,RUN_FAILED,false,3,PAUSED",
            "ARMED,RUN_SKIPPED,false,0,ARMED",
            "AWAITING_PUBLISH,BASE_PUBLISHED,false,0,ARMED",
            "AWAITING_PUBLISH,STILL_AWAITING,false,0,AWAITING_PUBLISH",
            "CONVERGED,RUN_SUCCEEDED,true,0,ARMED",
            "CONVERGED,RUN_FAILED,false,1,CONVERGED",
            "PAUSED,REARM,false,3,ARMED",
            "EXHAUSTED,REARM,false,0,ARMED",
            "CONVERGED,REARM,false,0,ARMED"})
    void transitionTable(String from, String signal, boolean autoPublish, int failures, String expected) {
        assertThat(transition.next(DdcAiPolicyStatePersistenceEnum.valueOf(from), Signal.valueOf(signal),
                new Context(autoPublish, failures)))
                .isEqualTo(DdcAiPolicyStatePersistenceEnum.valueOf(expected));
    }

    @Test
    void evaluationSignalsOutsideArmedAreUndefined() {
        assertThatThrownBy(() -> transition.next(DdcAiPolicyStatePersistenceEnum.CONVERGED, Signal.THRESHOLD_MET,
                new Context(false, 0))).isInstanceOf(IllegalStateException.class)
                .hasMessage("AI_POLICY_TRANSITION_UNDEFINED");
        assertThatThrownBy(() -> transition.next(DdcAiPolicyStatePersistenceEnum.PAUSED, Signal.RUN_CREATED,
                new Context(false, 0))).isInstanceOf(IllegalStateException.class);
    }
}
