package top.egon.cola.component.bytecode.core.enhance.accessguard;

import java.util.Set;

public final class GovernanceAnnotationFilter {

    private static final Set<String> ANNOTATIONS = Set.of(
            "Ltop/egon/cola/component/accessguard/api/AccessGuard;",
            "Ltop/egon/cola/component/accessguard/api/AllowListGuard;",
            "Ltop/egon/cola/component/accessguard/api/RateLimitGuard;",
            "Ltop/egon/cola/component/accessguard/api/TimeLimitGuard;"
    );

    public boolean isGovernance(String descriptor) {
        return ANNOTATIONS.contains(descriptor);
    }
}
