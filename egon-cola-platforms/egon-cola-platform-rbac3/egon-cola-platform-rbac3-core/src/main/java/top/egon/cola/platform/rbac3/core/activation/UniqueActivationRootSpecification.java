package top.egon.cola.platform.rbac3.core.activation;

import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.Set;

public final class UniqueActivationRootSpecification {

    public String requireUniqueRoot(String roleId, RoleHierarchy hierarchy) {
        Set<String> roots = hierarchy.rootsOf(roleId);
        if (roots.isEmpty()) {
            throw new Rbac3RuleViolation("ROLE_ACTIVATION_ROOT_MISSING", java.util.List.of(roleId));
        }
        if (roots.size() != 1) {
            throw new Rbac3RuleViolation("ROLE_ACTIVATION_ROOT_AMBIGUOUS",
                    roots.stream().sorted().toList());
        }
        return roots.iterator().next();
    }
}
