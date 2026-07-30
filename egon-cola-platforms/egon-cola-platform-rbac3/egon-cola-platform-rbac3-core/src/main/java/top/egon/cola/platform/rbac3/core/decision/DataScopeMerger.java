package top.egon.cola.platform.rbac3.core.decision;

import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class DataScopeMerger {

    public Map<String, NormalizedDataScope> merge(
            List<AuthorizationRuleFacts.DataScopeFact> facts,
            Set<String> effectiveRoleIds
    ) {
        var mutable = new TreeMap<String, MutableScope>();
        for (AuthorizationRuleFacts.DataScopeFact fact : facts) {
            if (!effectiveRoleIds.contains(fact.roleId())) {
                continue;
            }
            MutableScope scope = mutable.computeIfAbsent(
                    fact.permissionCode(), ignored -> new MutableScope());
            if (scope.directorySnapshotVersion != null
                    && scope.directorySnapshotVersion != fact.directorySnapshotVersion()) {
                throw new Rbac3RuleViolation("DATA_SCOPE_DIRECTORY_VERSION_CONFLICT");
            }
            scope.directorySnapshotVersion = fact.directorySnapshotVersion();
            if ("TENANT_ALL".equals(fact.dimension())) {
                scope.allInTenant = true;
            } else if (!"NONE".equals(fact.dimension())) {
                if (fact.referenceId() == null || fact.referenceId().isBlank()) {
                    throw new Rbac3RuleViolation("DATA_SCOPE_REFERENCE_MISSING");
                }
                scope.references.computeIfAbsent(fact.dimension(), ignored -> new TreeSet<>())
                        .add(fact.referenceId());
            }
        }
        var result = new TreeMap<String, NormalizedDataScope>();
        mutable.forEach((permission, scope) -> result.put(permission, scope.freeze()));
        return Collections.unmodifiableMap(result);
    }

    public record NormalizedDataScope(
            boolean allInTenant,
            Map<String, Set<String>> referencesByDimension,
            long directorySnapshotVersion
    ) {
    }

    private static final class MutableScope {
        private boolean allInTenant;
        private Long directorySnapshotVersion;
        private final Map<String, Set<String>> references = new TreeMap<>();

        private NormalizedDataScope freeze() {
            var copied = new TreeMap<String, Set<String>>();
            references.forEach((dimension, ids) -> copied.put(
                    dimension, Collections.unmodifiableSet(new TreeSet<>(ids))));
            return new NormalizedDataScope(allInTenant,
                    Collections.unmodifiableMap(copied),
                    directorySnapshotVersion == null ? 0 : directorySnapshotVersion);
        }
    }
}
