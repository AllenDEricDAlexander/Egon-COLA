package top.egon.cola.platform.rbac3.core.activation;

import java.util.Set;
import java.util.TreeSet;

public record DsdSetFact(
        String id,
        String applicationId,
        int maxActiveRoles,
        Set<String> rootRoleIds
) {

    public DsdSetFact {
        id = required(id, "id");
        applicationId = required(applicationId, "applicationId");
        if (maxActiveRoles < 1) {
            throw new IllegalArgumentException("maxActiveRoles must be positive");
        }
        rootRoleIds = Set.copyOf(new TreeSet<>(rootRoleIds));
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
