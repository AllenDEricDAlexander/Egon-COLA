package top.egon.cola.component.accessguard.core.plan;

import java.time.Instant;

public record GuardPlanChangedEvent(
        String ruleId,
        long previousVersion,
        long currentVersion,
        String source,
        Instant changedAt
) {
}
