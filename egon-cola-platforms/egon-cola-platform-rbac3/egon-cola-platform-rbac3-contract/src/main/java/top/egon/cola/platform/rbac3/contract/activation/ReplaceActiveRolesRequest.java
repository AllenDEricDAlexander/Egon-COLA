package top.egon.cola.platform.rbac3.contract.activation;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ReplaceActiveRolesRequest(
        List<String> roleIds,
        long expectedSessionVersion
) {

    public ReplaceActiveRolesRequest {
        roleIds = List.copyOf(Objects.requireNonNull(roleIds, "roleIds"));
        if (roleIds.isEmpty()) {
            throw new IllegalArgumentException("roleIds is required");
        }
        roleIds.forEach(roleId -> required(roleId, "roleIds"));
        if (Set.copyOf(roleIds).size() != roleIds.size()) {
            throw new IllegalArgumentException(
                    "roleIds must not contain duplicates"
            );
        }
        if (expectedSessionVersion < 0L) {
            throw new IllegalArgumentException(
                    "expectedSessionVersion must not be negative"
            );
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
