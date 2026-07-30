package top.egon.cola.platform.rbac3.core.decision;

import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class LandingRouteSelector {

    public Optional<String> select(
            List<AuthorizationRuleFacts.LandingRouteFact> routes,
            Set<String> effectiveRoleIds,
            Set<String> permissionCodes
    ) {
        return routes.stream()
                .filter(route -> effectiveRoleIds.contains(route.roleId()))
                .filter(route -> permissionCodes.contains(route.requiredPermissionCode()))
                .sorted(Comparator.comparingInt(AuthorizationRuleFacts.LandingRouteFact::priority)
                        .thenComparing(AuthorizationRuleFacts.LandingRouteFact::routeCode))
                .map(AuthorizationRuleFacts.LandingRouteFact::routeCode)
                .findFirst();
    }
}
