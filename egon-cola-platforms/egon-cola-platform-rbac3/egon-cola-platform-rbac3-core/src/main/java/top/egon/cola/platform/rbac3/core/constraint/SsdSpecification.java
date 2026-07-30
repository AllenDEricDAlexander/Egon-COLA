package top.egon.cola.platform.rbac3.core.constraint;

import top.egon.cola.platform.rbac3.core.rule.RuleResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SsdSpecification {

    public RuleResult evaluate(Set<String> resultingRoleIds, List<SsdSet> sets) {
        for (SsdSet set : sets) {
            var matched = new ArrayList<String>();
            for (String roleId : resultingRoleIds) {
                if (set.roleIds().contains(roleId)) {
                    matched.add(roleId);
                }
            }
            if (matched.size() > set.maxAssignedRoles()) {
                matched.add(set.id());
                return RuleResult.deny("SSD_CONSTRAINT_VIOLATION", matched);
            }
        }
        return RuleResult.allow("SSD_CONSTRAINT_SATISFIED");
    }

    public record SsdSet(String id, int maxAssignedRoles, Set<String> roleIds) {
        public SsdSet {
            if (id == null || id.isBlank() || maxAssignedRoles < 1) {
                throw new IllegalArgumentException("valid SSD id and maximum are required");
            }
            roleIds = Set.copyOf(roleIds);
        }
    }
}
