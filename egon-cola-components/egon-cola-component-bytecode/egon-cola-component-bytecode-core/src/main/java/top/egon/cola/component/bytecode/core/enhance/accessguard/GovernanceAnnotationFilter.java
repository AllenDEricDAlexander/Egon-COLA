package top.egon.cola.component.bytecode.core.enhance.accessguard;

import java.util.Set;

public final class GovernanceAnnotationFilter {

    private static final Set<String> ANNOTATIONS = Set.of(
            "Ltop/egon/cola/component/accessguard/annotation/AccessGuard;",
            "Ltop/egon/cola/component/accessguard/annotation/WhiteListAccessInterceptor;",
            "Ltop/egon/cola/component/accessguard/annotation/RateLimiterAccessInterceptor;",
            "Ltop/egon/cola/component/accessguard/annotation/TimeoutCircuitBreaker;",
            "Ltop/egon/cola/component/accessguard/annotation/DoWhiteList;",
            "Ltop/egon/cola/component/accessguard/annotation/DoRateLimiter;",
            "Ltop/egon/cola/component/accessguard/annotation/DoHystrix;"
    );

    public boolean isGovernance(String descriptor) {
        return ANNOTATIONS.contains(descriptor);
    }
}
