package top.egon.cola.platform.rbac3.core.constraint;

import top.egon.cola.platform.rbac3.core.rule.RuleResult;

import java.util.List;
import java.util.Set;

public final class PrerequisiteRoleSpecification {

    public RuleResult evaluate(Set<String> resultingRoleIds, PrerequisiteGroup group) {
        boolean satisfied = switch (group.matchMode()) {
            case ALL_OF -> resultingRoleIds.containsAll(group.requiredRoleIds());
            case ANY_OF -> group.requiredRoleIds().stream().anyMatch(resultingRoleIds::contains);
        };
        return satisfied
                ? RuleResult.allow("PREREQUISITE_ROLE_SATISFIED")
                : RuleResult.deny("PREREQUISITE_ROLE_MISSING", List.of(group.id()));
    }

    public record PrerequisiteGroup(
            String id,
            MatchMode matchMode,
            Set<String> requiredRoleIds
    ) {
        public PrerequisiteGroup {
            if (id == null || id.isBlank() || matchMode == null
                    || requiredRoleIds.isEmpty()) {
                throw new IllegalArgumentException("complete prerequisite group is required");
            }
            requiredRoleIds = Set.copyOf(requiredRoleIds);
        }
    }

    public enum MatchMode {
        ALL_OF,
        ANY_OF
    }
}
