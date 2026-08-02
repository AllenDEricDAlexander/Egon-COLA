package top.egon.cola.platform.rbac3.contract.activation;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ReplaceActiveRolesRequest(
        List<String> roleIds,
        long expectedContextVersion
) {

    public ReplaceActiveRolesRequest {
        roleIds = Objects.requireNonNull(roleIds, "roleIds")
                .stream()
                .map(ReplaceActiveRolesRequest::canonicalRoleId)
                .toList();
        if (roleIds.isEmpty()) {
            throw new IllegalArgumentException("roleIds is required");
        }
        if (Set.copyOf(roleIds).size() != roleIds.size()) {
            throw new IllegalArgumentException(
                    "roleIds must not contain duplicates"
            );
        }
        if (expectedContextVersion < 0L) {
            throw new IllegalArgumentException(
                    "expectedContextVersion must not be negative"
            );
        }
    }

    private static String canonicalRoleId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("roleIds is required");
        }
        String candidate = value.trim();
        if (!candidate.chars().allMatch(
                character -> character >= '0' && character <= '9')) {
            throw new IllegalArgumentException(
                    "roleIds must contain decimal identifiers"
            );
        }
        try {
            long identifier = Long.parseLong(candidate);
            if (identifier <= 0L) {
                throw new IllegalArgumentException(
                        "roleIds must contain positive identifiers"
                );
            }
            return Long.toString(identifier);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "roleIds must contain valid decimal identifiers",
                    exception
            );
        }
    }
}
