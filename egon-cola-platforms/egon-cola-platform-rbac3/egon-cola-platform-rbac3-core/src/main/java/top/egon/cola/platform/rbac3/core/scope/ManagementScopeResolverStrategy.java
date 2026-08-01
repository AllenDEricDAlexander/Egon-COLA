package top.egon.cola.platform.rbac3.core.scope;

import java.util.Set;

public interface ManagementScopeResolverStrategy {

    ScopeType type();

    Set<String> resolve(ScopeInput input);

    record ScopeInput(
            String subjectUserId,
            String targetUserId,
            String referenceId,
            Set<String> subjectAffiliations,
            Set<String> targetAffiliations
    ) {
        public ScopeInput {
            subjectAffiliations = Set.copyOf(subjectAffiliations);
            targetAffiliations = Set.copyOf(targetAffiliations);
        }
    }

    enum ScopeType {
        SELF_DEPT,
        DEPT,
        DEPT_TREE,
        ORG,
        ORG_TREE,
        CUSTOM_DEPT,
        CUSTOM_USER
    }
}
