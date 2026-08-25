package top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.vo;

import java.util.List;
import java.util.Set;

/** Common response for an atomic direct-resource replacement. */
public record RoleResourceGrantMutationVO(
        String roleId,
        long roleVersion,
        List<String> directResourceIds,
        List<String> derivedApiResourceIds,
        List<String> effectiveResourceIds,
        long addedCount,
        long removedCount,
        long effectivePermissionCount,
        long authVersion,
        long policyVersion) {

    public RoleResourceGrantMutationVO {
        roleId = required(roleId, "roleId");
        if (roleVersion < 0L || authVersion < 0L || policyVersion < 0L
                || addedCount < 0L || removedCount < 0L
                || effectivePermissionCount < 0L) {
            throw new IllegalArgumentException("mutation counts and versions must not be negative");
        }
        directResourceIds = sorted(directResourceIds);
        derivedApiResourceIds = sorted(derivedApiResourceIds);
        effectiveResourceIds = sorted(effectiveResourceIds);
    }

    public static RoleResourceGrantMutationVO success(
            Long roleId,
            long roleVersion,
            Set<Long> directResourceIds,
            Set<Long> derivedApiResourceIds,
            Set<Long> effectiveResourceIds,
            long addedCount,
            long removedCount,
            long effectivePermissionCount,
            long authVersion,
            long policyVersion) {
        return new RoleResourceGrantMutationVO(
                String.valueOf(roleId), roleVersion,
                ids(directResourceIds), ids(derivedApiResourceIds),
                ids(effectiveResourceIds), addedCount, removedCount,
                effectivePermissionCount, authVersion, policyVersion);
    }

    private static List<String> ids(Set<Long> ids) {
        return ids.stream().sorted().map(String::valueOf).toList();
    }

    private static List<String> sorted(List<String> values) {
        return values == null ? List.of() : values.stream().sorted().toList();
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
