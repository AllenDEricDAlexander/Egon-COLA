package top.egon.cola.component.tianshu.admin.service.ai.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiPolicyStatePersistenceEnum;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import static top.egon.cola.component.tianshu.admin.model.enums.DdcAiPolicyStatePersistenceEnum.ARMED;
import static top.egon.cola.component.tianshu.admin.model.enums.DdcAiPolicyStatePersistenceEnum.AWAITING_PUBLISH;
import static top.egon.cola.component.tianshu.admin.model.enums.DdcAiPolicyStatePersistenceEnum.CONVERGED;
import static top.egon.cola.component.tianshu.admin.model.enums.DdcAiPolicyStatePersistenceEnum.EXHAUSTED;
import static top.egon.cola.component.tianshu.admin.model.enums.DdcAiPolicyStatePersistenceEnum.PAUSED;

/**
 * 优化策略的表驱动状态机（Spec §10.6）。评估信号只作用于 ARMED；运行结果与 REARM 作用于所有状态。
 * / Table-driven state machine of optimization policies (Spec §10.6). Evaluation signals apply to ARMED only; run
 * outcomes and REARM apply to every state.
 */
@Slf4j
@Component("ddcAiOptimizationStateTransition")
public class DdcAiOptimizationStateTransition {

    /** 连续失败达到该次数后暂停。 / Number of consecutive failures that pauses the policy. */
    public static final int PAUSE_AFTER_FAILURES = 3;

    /** 状态机输入。 / State machine input. */
    public enum Signal {
        NOT_PUBLISHED, INSUFFICIENT_SAMPLES, COOLING_DOWN, RUN_CREATED, THRESHOLD_MET, ITERATIONS_EXHAUSTED,
        BASE_PUBLISHED, STILL_AWAITING, RUN_SUCCEEDED, RUN_FAILED, RUN_SKIPPED, REARM
    }

    /** 转移所需的策略上下文。 / Policy context needed by a transition. */
    public record Context(boolean autoPublish, int consecutiveFailures) {
    }

    private static final Map<DdcAiPolicyStatePersistenceEnum, Map<Signal, Function<Context, DdcAiPolicyStatePersistenceEnum>>>
            EVALUATION = new EnumMap<>(Map.of(
                    ARMED, new EnumMap<>(Map.of(
                            Signal.NOT_PUBLISHED, context -> ARMED,
                            Signal.INSUFFICIENT_SAMPLES, context -> ARMED,
                            Signal.COOLING_DOWN, context -> ARMED,
                            Signal.RUN_CREATED, context -> ARMED,
                            Signal.THRESHOLD_MET, context -> CONVERGED,
                            Signal.ITERATIONS_EXHAUSTED, context -> EXHAUSTED)),
                    AWAITING_PUBLISH, new EnumMap<>(Map.of(
                            Signal.BASE_PUBLISHED, context -> ARMED,
                            Signal.STILL_AWAITING, context -> AWAITING_PUBLISH,
                            Signal.NOT_PUBLISHED, context -> AWAITING_PUBLISH))));

    public DdcAiPolicyStatePersistenceEnum next(DdcAiPolicyStatePersistenceEnum state, Signal signal, Context context) {
        return switch (signal) {
            case REARM -> ARMED;
            case RUN_SUCCEEDED -> context.autoPublish() ? ARMED : AWAITING_PUBLISH;
            case RUN_FAILED -> context.consecutiveFailures() >= PAUSE_AFTER_FAILURES ? PAUSED : state;
            case RUN_SKIPPED -> state;
            default -> {
                Function<Context, DdcAiPolicyStatePersistenceEnum> transition =
                        EVALUATION.getOrDefault(state, Map.of()).get(signal);
                if (transition == null) throw new IllegalStateException("AI_POLICY_TRANSITION_UNDEFINED");
                yield transition.apply(context);
            }
        };
    }
}
