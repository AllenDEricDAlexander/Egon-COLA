package top.egon.cola.platform.rbac3.contract.authorization;

import top.egon.cola.platform.rbac3.contract.manifest.ManifestResource;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record AppAuthorizationContext(
        String applicationId,
        String applicationCode,
        List<String> activationRootRoleIds,
        List<String> eligibleAssignmentIds,
        List<String> effectiveRoleIds,
        Set<String> permissions,
        Map<String, DataScopeDecision> dataScopes,
        Map<String, FieldPolicyDecision> fieldPolicies,
        List<ManifestResource> resources,
        String landingRouteCode
) {

    public AppAuthorizationContext {
        applicationId = required(applicationId, "applicationId");
        applicationCode = required(applicationCode, "applicationCode");
        activationRootRoleIds = immutableIds(
                activationRootRoleIds,
                "activationRootRoleIds"
        );
        eligibleAssignmentIds = immutableIds(
                eligibleAssignmentIds,
                "eligibleAssignmentIds"
        );
        effectiveRoleIds = immutableIds(
                effectiveRoleIds,
                "effectiveRoleIds"
        );
        permissions = Set.copyOf(Objects.requireNonNull(
                permissions,
                "permissions"
        ));
        permissions.forEach(code -> required(code, "permissions"));
        dataScopes = Map.copyOf(Objects.requireNonNull(
                dataScopes,
                "dataScopes"
        ));
        dataScopes.keySet().forEach(code -> required(code, "dataScopes"));
        fieldPolicies = Map.copyOf(Objects.requireNonNull(
                fieldPolicies,
                "fieldPolicies"
        ));
        fieldPolicies.keySet().forEach(code -> required(
                code,
                "fieldPolicies"
        ));
        resources = List.copyOf(Objects.requireNonNull(
                resources,
                "resources"
        ));
        landingRouteCode = optional(
                landingRouteCode,
                "landingRouteCode"
        );
    }

    private static List<String> immutableIds(
            List<String> values,
            String fieldName) {
        List<String> copy = List.copyOf(Objects.requireNonNull(
                values,
                fieldName
        ));
        copy.forEach(value -> required(value, fieldName));
        return copy;
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
}
