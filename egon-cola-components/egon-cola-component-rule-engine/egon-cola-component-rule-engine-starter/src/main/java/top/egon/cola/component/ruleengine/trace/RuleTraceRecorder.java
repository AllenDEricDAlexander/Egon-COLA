package top.egon.cola.component.ruleengine.trace;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RuleTraceRecorder {

    private final boolean enabled;

    private final List<NodeTrace> nodeTraces = new ArrayList<>();

    public RuleTraceRecorder(boolean enabled) {
        this.enabled = enabled;
    }

    public void addNodeTrace(NodeTrace nodeTrace) {
        if (enabled && nodeTrace != null) {
            nodeTraces.add(nodeTrace);
        }
    }

    public RuleTrace finish(String ruleCode, String ruleName, String modelType, RuleContext context,
                            RuleStatus status, Throwable error) {
        Instant endTime = Instant.now();
        String errorMessage = error == null ? null : error.getMessage();
        return new RuleTrace(ruleCode, ruleName, modelType, context.getRequestId(), context.getTraceId(),
                context.getStartTime(), endTime, Duration.between(context.getStartTime(), endTime).toMillis(),
                status, enabled ? List.copyOf(nodeTraces) : List.of(), errorMessage);
    }
}
