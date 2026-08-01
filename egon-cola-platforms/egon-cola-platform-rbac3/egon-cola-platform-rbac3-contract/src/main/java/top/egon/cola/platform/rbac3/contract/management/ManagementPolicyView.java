package top.egon.cola.platform.rbac3.contract.management;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ManagementPolicyView(
        String policyId,
        String policyCode,
        String name,
        Instant validFrom,
        Instant validTo,
        List<Subject> subjects,
        List<Scope> scopes,
        List<String> activationRootRoleIds,
        Set<String> operations,
        Restrictions restrictions,
        long version,
        long policyVersion
) {

    public ManagementPolicyView {
        policyId = required(policyId, "policyId");
        policyCode = required(policyCode, "policyCode");
        name = required(name, "name");
        validFrom = Objects.requireNonNull(validFrom, "validFrom");
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException(
                    "validTo must be after validFrom"
            );
        }
        subjects = requiredList(subjects, "subjects");
        scopes = requiredList(scopes, "scopes");
        activationRootRoleIds = requiredIds(
                activationRootRoleIds,
                "activationRootRoleIds"
        );
        operations = Set.copyOf(Objects.requireNonNull(
                operations,
                "operations"
        ));
        if (operations.isEmpty()) {
            throw new IllegalArgumentException("operations is required");
        }
        operations.forEach(operation -> required(
                operation,
                "operations"
        ));
        restrictions = Objects.requireNonNull(
                restrictions,
                "restrictions"
        );
        nonNegative(version, "version");
        nonNegative(policyVersion, "policyVersion");
    }

    public record Subject(
            String type,
            String id
    ) {

        public Subject {
            type = required(type, "subject.type");
            id = required(id, "subject.id");
        }
    }

    public record Scope(
            String type,
            String refId
    ) {

        public Scope {
            type = required(type, "scope.type");
            refId = optional(refId, "scope.refId");
        }
    }

    public record Restrictions(
            Integer maxAssignmentDays,
            String maxRiskLevel,
            String requiredAuthStrength,
            boolean requireReason,
            boolean requireTicket,
            boolean includeInheritedSubjectRoles,
            boolean requireAllAffiliationsInScope,
            Set<String> allowedRoleTypes
    ) {

        public Restrictions {
            if (maxAssignmentDays != null && maxAssignmentDays < 1) {
                throw new IllegalArgumentException(
                        "maxAssignmentDays must be positive"
                );
            }
            maxRiskLevel = required(maxRiskLevel, "maxRiskLevel");
            requiredAuthStrength = required(
                    requiredAuthStrength,
                    "requiredAuthStrength"
            );
            allowedRoleTypes = Set.copyOf(Objects.requireNonNull(
                    allowedRoleTypes,
                    "allowedRoleTypes"
            ));
            allowedRoleTypes.forEach(type -> required(
                    type,
                    "allowedRoleTypes"
            ));
        }
    }

    private static <T> List<T> requiredList(
            List<T> values,
            String fieldName) {
        List<T> copy = List.copyOf(Objects.requireNonNull(
                values,
                fieldName
        ));
        if (copy.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return copy;
    }

    private static List<String> requiredIds(
            List<String> values,
            String fieldName) {
        List<String> copy = requiredList(values, fieldName);
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

    private static void nonNegative(long value, String fieldName) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                    fieldName + " must not be negative"
            );
        }
    }
}
