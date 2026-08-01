package top.egon.cola.platform.rbac3.core.participation;

import top.egon.cola.platform.rbac3.core.rule.RuleResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OperationSodSpecification {

    public RuleResult evaluate(
            String businessResource,
            String businessId,
            String actorUserId,
            String requestedAction,
            List<ParticipationFact> participationFacts,
            Map<String, Set<String>> conflictingPriorActions
    ) {
        Set<String> conflicts = conflictingPriorActions.getOrDefault(
                requestedAction, Set.of());
        var evidence = new ArrayList<String>();
        for (ParticipationFact fact : participationFacts) {
            if (fact.businessResource().equals(businessResource)
                    && fact.businessId().equals(businessId)
                    && fact.actorUserId().equals(actorUserId)
                    && conflicts.contains(fact.actionCode())) {
                evidence.add(fact.eventId());
            }
        }
        return evidence.isEmpty()
                ? RuleResult.allow("OPERATION_SOD_SATISFIED")
                : RuleResult.deny("OPERATION_SOD_CONSTRAINT_VIOLATION", evidence);
    }

    public record ParticipationFact(
            String eventId,
            String businessResource,
            String businessId,
            String actorUserId,
            String actionCode
    ) {
    }
}
