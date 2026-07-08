package top.egon.cola.component.ruleengine.trace;

import top.egon.cola.component.ruleengine.result.RuleStatus;
import top.egon.cola.component.ruleengine.tree.NodeType;

import java.time.Instant;

public record NodeTrace(
        String nodeCode,
        String nodeName,
        NodeType nodeType,
        int order,
        int visitCount,
        Instant startTime,
        Instant endTime,
        long costMillis,
        String routeTo,
        String routeReason,
        RuleStatus status,
        String error
) {
}
