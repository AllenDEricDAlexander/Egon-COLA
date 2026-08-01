package top.egon.cola.platform.rbac3.core.activation;

import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public final class ApplicationRoleMutexSpecification {

    public void requireAllowed(
            Map<String, Set<String>> rootsByApplication,
            java.util.List<DsdSetFact> dsdSets
    ) {
        for (DsdSetFact dsdSet : dsdSets) {
            Set<String> active = rootsByApplication.getOrDefault(
                    dsdSet.applicationId(), Set.of());
            var matched = new ArrayList<String>();
            for (String rootId : active) {
                if (dsdSet.rootRoleIds().contains(rootId)) {
                    matched.add(rootId);
                }
            }
            if (matched.size() > dsdSet.maxActiveRoles()) {
                matched.add(dsdSet.id());
                throw new Rbac3RuleViolation(
                        "APP_ROLE_ACTIVATION_MUTEX_VIOLATION",
                        matched.stream().sorted().toList()
                );
            }
        }
    }
}
