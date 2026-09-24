package top.egon.cola.component.outbox.statemachine;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.autoconfigure.OutboxStateMachineProperties;
import top.egon.cola.component.outbox.common.exception.OutboxStateMachineException;
import top.egon.cola.component.outbox.store.OutboxStatus;

import java.util.Map;
import java.util.Set;

/** Applies the fixed technical lifecycle graph without performing persistence. */
@Slf4j
@Validated
@RequiredArgsConstructor
public class OutboxLifecycleService {

    private static final Set<String> PERSISTED_STATES = Set.of(
            OutboxStatus.PENDING.getMessage(),
            OutboxStatus.PROCESSING.getMessage(),
            OutboxStatus.RETRY_WAIT.getMessage(),
            OutboxStatus.SUCCEEDED.getMessage(),
            OutboxStatus.DEAD.getMessage()
    );

    private static final Map<String, OutboxStatus> STATUSES_BY_MESSAGE = Map.of(
            OutboxStatus.PENDING.getMessage(), OutboxStatus.PENDING,
            OutboxStatus.PROCESSING.getMessage(), OutboxStatus.PROCESSING,
            OutboxStatus.RETRY_WAIT.getMessage(), OutboxStatus.RETRY_WAIT,
            OutboxStatus.SUCCEEDED.getMessage(), OutboxStatus.SUCCEEDED,
            OutboxStatus.DEAD.getMessage(), OutboxStatus.DEAD
    );

    @Qualifier("outboxLifecycleStateMachineFactory")
    private final OutboxLifecycleStateMachineFactory factory;

    @Qualifier("outboxStateMachineExecutionService")
    private final StateMachineExecutionService executionService;

    @Qualifier("outboxStateMachineProperties")
    private final OutboxStateMachineProperties properties;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    public OutboxStatus evaluate(
            @NotNull String source,
            @NotNull OutboxLifecycleSignalEnum signal,
            @Min(0) int attemptCount,
            @Min(1) int maxAttempts
    ) {
        if (source == null || source.isBlank() || signal == null || attemptCount < 0 || maxAttempts < 1) {
            throw new OutboxStateMachineException(
                    "OUTBOX_FSM_INPUT_INVALID",
                    false,
                    "Invalid outbox lifecycle evaluation input"
            );
        }
        validationUtils.validate(properties);
        boolean newMessage = OutboxLifecycleStateMachineFactory.NEW_STATE.equals(source);
        if (!newMessage && !PERSISTED_STATES.contains(source)) {
            throw rejected("Unknown outbox lifecycle source state");
        }
        if (newMessage != (signal == OutboxLifecycleSignalEnum.ENQUEUE)) {
            throw rejected("Outbox enqueue signal is valid only for a new message");
        }

        String target = executionService.execute(
                factory.create(),
                source,
                MessageBuilder.withPayload(signal)
                        .copyHeaders(Map.of(
                                "attemptCount", attemptCount,
                                "maxAttempts", maxAttempts
                        ))
                        .build(),
                properties.getExecutionTimeout()
        );
        if (target == null) {
            throw new OutboxStateMachineException(
                    "OUTBOX_FSM_EXECUTION_FAILED",
                    false,
                    "Outbox state machine returned no target state"
            );
        }
        OutboxStatus status = STATUSES_BY_MESSAGE.get(target);
        if (status == null) {
            throw new OutboxStateMachineException(
                    "OUTBOX_FSM_EXECUTION_FAILED",
                    false,
                    "Outbox state machine returned an unknown target state"
            );
        }
        return status;
    }

    private static OutboxStateMachineException rejected(String message) {
        return new OutboxStateMachineException("OUTBOX_FSM_REJECTED", false, message);
    }
}
