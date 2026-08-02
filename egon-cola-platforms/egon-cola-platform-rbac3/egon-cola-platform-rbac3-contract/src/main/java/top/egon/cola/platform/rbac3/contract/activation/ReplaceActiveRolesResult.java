package top.egon.cola.platform.rbac3.contract.activation;

import java.util.List;
import java.util.Objects;

public record ReplaceActiveRolesResult(
        List<ActiveRoleSetView.ApplicationActiveRoles> activeRoles,
        boolean changed,
        long contextVersion,
        long authVersion,
        long policyVersion,
        boolean bootstrapRequired,
        String snapshotChecksum
) {

    public ReplaceActiveRolesResult {
        activeRoles = List.copyOf(Objects.requireNonNull(
                activeRoles,
                "activeRoles"
        ));
        nonNegative(contextVersion, "contextVersion");
        nonNegative(authVersion, "authVersion");
        nonNegative(policyVersion, "policyVersion");
        snapshotChecksum = required(
                snapshotChecksum,
                "snapshotChecksum"
        );
    }

    @Override
    public String toString() {
        return "ReplaceActiveRolesResult[activeRoles=" + activeRoles
                + ", changed=" + changed
                + ", contextVersion=" + contextVersion
                + ", authVersion=" + authVersion
                + ", policyVersion=" + policyVersion
                + ", bootstrapRequired=" + bootstrapRequired
                + ", snapshotChecksum=" + snapshotChecksum + ']';
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
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
