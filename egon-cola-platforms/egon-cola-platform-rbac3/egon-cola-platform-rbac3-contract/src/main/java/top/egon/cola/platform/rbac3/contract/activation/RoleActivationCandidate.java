package top.egon.cola.platform.rbac3.contract.activation;

import java.util.List;
import java.util.Objects;

public record RoleActivationCandidate(
        String rootRoleId,
        String rootRoleCode,
        String displayName,
        List<String> sourceRoleIds,
        List<String> eligibleAssignmentIds,
        List<String> mutexSetIds,
        String effectiveFamilyRisk,
        String requiredAuthStrength,
        String landingRouteCode
) {

    public RoleActivationCandidate {
        rootRoleId = required(rootRoleId, "rootRoleId");
        rootRoleCode = required(rootRoleCode, "rootRoleCode");
        displayName = required(displayName, "displayName");
        sourceRoleIds = requiredIds(sourceRoleIds, "sourceRoleIds");
        eligibleAssignmentIds = requiredIds(
                eligibleAssignmentIds,
                "eligibleAssignmentIds"
        );
        mutexSetIds = List.copyOf(Objects.requireNonNull(
                mutexSetIds,
                "mutexSetIds"
        ));
        mutexSetIds.forEach(id -> required(id, "mutexSetIds"));
        effectiveFamilyRisk = required(
                effectiveFamilyRisk,
                "effectiveFamilyRisk"
        );
        requiredAuthStrength = required(
                requiredAuthStrength,
                "requiredAuthStrength"
        );
        landingRouteCode = optional(
                landingRouteCode,
                "landingRouteCode"
        );
    }

    private static List<String> requiredIds(
            List<String> values,
            String fieldName) {
        List<String> copy = List.copyOf(Objects.requireNonNull(
                values,
                fieldName
        ));
        if (copy.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
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
