package top.egon.cola.platform.rbac3.core.activation;

import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class RoleActivationCandidateResolver {

    public static final int MAX_CANDIDATE_ROOTS = 512;

    public Map<String, Set<String>> resolve(
            java.util.List<EligibleAssignmentFact> assignments,
            RoleHierarchy hierarchy,
            Instant databaseNow
    ) {
        var byRoot = new TreeMap<String, Set<String>>();
        var uniqueRoot = new UniqueActivationRootSpecification();
        for (EligibleAssignmentFact assignment : assignments) {
            if (!assignment.eligibleAt(databaseNow)) {
                continue;
            }
            String root = uniqueRoot.requireUniqueRoot(assignment.roleId(), hierarchy);
            byRoot.computeIfAbsent(root, ignored -> new TreeSet<>()).add(assignment.id());
            if (byRoot.size() > MAX_CANDIDATE_ROOTS) {
                throw new Rbac3RuleViolation("ROLE_ACTIVATION_CANDIDATE_LIMIT_EXCEEDED");
            }
        }
        var result = new TreeMap<String, Set<String>>();
        byRoot.forEach((root, ids) -> result.put(root,
                Collections.unmodifiableSet(new TreeSet<>(ids))));
        return Collections.unmodifiableMap(result);
    }
}
