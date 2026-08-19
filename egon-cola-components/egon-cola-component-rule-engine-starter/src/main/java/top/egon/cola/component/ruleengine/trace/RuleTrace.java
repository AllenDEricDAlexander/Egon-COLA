package top.egon.cola.component.ruleengine.trace;

import top.egon.cola.component.ruleengine.result.RuleStatus;

import java.time.Instant;
import java.util.List;

public record RuleTrace(
        String ruleCode,
        String ruleName,
        String modelType,
        String requestId,
        String traceId,
        Instant startTime,
        Instant endTime,
        long costMillis,
        RuleStatus status,
        List<NodeTrace> nodeTraces,
        String error
) {
}
