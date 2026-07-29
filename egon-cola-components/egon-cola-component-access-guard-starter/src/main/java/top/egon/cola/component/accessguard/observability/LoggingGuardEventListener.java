package top.egon.cola.component.accessguard.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.accessguard.core.GuardFailure;
import top.egon.cola.component.accessguard.core.GuardOutcome;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LoggingGuardEventListener implements GuardEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingGuardEventListener.class);

    @Override
    public void onEvent(GuardEvent event) {
        if (!event.loggingEnabled()) {
            return;
        }
        Map<String, Object> fields = fields(event);
        if (event.outcome().failure() == null) {
            LOGGER.debug("Access Guard outcome {}", fields);
        } else {
            LOGGER.warn("Access Guard outcome {}", fields);
        }
    }

    @Override
    public void onStage(GuardStageEvent event) {
        if (!event.loggingEnabled()) {
            return;
        }
        LOGGER.debug("Access Guard stage stage={} ruleId={} planVersion={} policy={} type={} decision={}",
                event.stage(),
                event.outcome().ruleId(),
                event.outcome().planVersion(),
                event.outcome().policy(),
                event.outcome().type(),
                event.outcome().decision());
    }

    Map<String, Object> fields(GuardEvent event) {
        GuardOutcome outcome = event.outcome();
        GuardFailure failure = outcome.failure();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("ruleId", outcome.ruleId());
        fields.put("planVersion", outcome.planVersion());
        fields.put("policy", outcome.policy());
        fields.put("type", outcome.type());
        fields.put("decision", outcome.decision());
        fields.put("resolution", outcome.resolution());
        fields.put("engine", outcome.engine());
        fields.put("storage", outcome.storage());
        fields.put("elapsed", outcome.elapsed());
        fields.put("retryAfter", outcome.retryAfter());
        fields.put("failureCategory", failure == null ? "" : failure.category());
        fields.put("failureCode", failure == null ? "" : failure.code());
        return Map.copyOf(fields);
    }
}
