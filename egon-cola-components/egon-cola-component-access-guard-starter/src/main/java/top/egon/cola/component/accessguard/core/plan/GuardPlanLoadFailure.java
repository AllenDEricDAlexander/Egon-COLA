package top.egon.cola.component.accessguard.core.plan;

import java.time.Instant;

public record GuardPlanLoadFailure(
        String ruleId,
        String source,
        long attemptedVersion,
        String code,
        Instant occurredAt
) {
}
