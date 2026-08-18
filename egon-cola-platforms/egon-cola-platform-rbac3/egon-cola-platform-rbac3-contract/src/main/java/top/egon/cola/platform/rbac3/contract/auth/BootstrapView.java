package top.egon.cola.platform.rbac3.contract.auth;

import top.egon.cola.platform.rbac3.contract.activation.ActivationRoot;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record BootstrapView(
        User user,
        List<ActiveRoleContext> activeRoleContexts,
        Set<String> permissions,
        List<String> apps,
        List<String> menus,
        List<String> routes,
        List<String> actions,
        Map<String, FieldPolicyDecision> fieldPolicies,
        String defaultApplicationCode,
        String defaultRoute,
        long authVersion,
        long policyVersion
) {

    public BootstrapView {
        user = Objects.requireNonNull(user, "user");
        activeRoleContexts = List.copyOf(Objects.requireNonNull(
                activeRoleContexts,
                "activeRoleContexts"
        ));
        permissions = Set.copyOf(Objects.requireNonNull(
                permissions,
                "permissions"
        ));
        apps = List.copyOf(Objects.requireNonNull(apps, "apps"));
        menus = List.copyOf(Objects.requireNonNull(menus, "menus"));
        routes = List.copyOf(Objects.requireNonNull(routes, "routes"));
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        fieldPolicies = Map.copyOf(Objects.requireNonNull(
                fieldPolicies,
                "fieldPolicies"
        ));
        defaultApplicationCode = optional(
                defaultApplicationCode,
                "defaultApplicationCode"
        );
        defaultRoute = optional(defaultRoute, "defaultRoute");
        nonNegative(authVersion, "authVersion");
        nonNegative(policyVersion, "policyVersion");
    }

    public record User(
            String id,
            String tenantId,
            String identitySub,
            String status
    ) {

        public User {
            id = required(id, "user.id");
            tenantId = required(tenantId, "user.tenantId");
            identitySub = required(identitySub, "user.identitySub");
            status = required(status, "user.status");
        }
    }

    public record ActiveRoleContext(
            String applicationCode,
            ActivationRoot activationRoot,
            List<String> effectiveRoleIds,
            List<String> eligibleAssignmentIds,
            String landingRoute
    ) {

        public ActiveRoleContext {
            applicationCode = required(
                    applicationCode,
                    "activeRoleContext.applicationCode"
            );
            activationRoot = Objects.requireNonNull(
                    activationRoot,
                    "activationRoot"
            );
            effectiveRoleIds = List.copyOf(Objects.requireNonNull(
                    effectiveRoleIds,
                    "effectiveRoleIds"
            ));
            eligibleAssignmentIds = List.copyOf(Objects.requireNonNull(
                    eligibleAssignmentIds,
                    "eligibleAssignmentIds"
            ));
            landingRoute = optional(landingRoute, "landingRoute");
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String optional(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }
        return value.trim();
    }

    private static void nonNegative(long value, String fieldName) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                    fieldName + " must not be negative"
            );
        }
    }
}
