package top.egon.cola.platform.rbac3.contract.activation;

import java.util.List;
import java.util.Objects;

public record ActiveRoleSetView(
        List<ApplicationActiveRoles> activeRoles,
        boolean activationRequired,
        long authVersion,
        long policyVersion,
        String snapshotChecksum
) {

    public ActiveRoleSetView {
        activeRoles = List.copyOf(Objects.requireNonNull(
                activeRoles,
                "activeRoles"
        ));
        nonNegative(authVersion, "authVersion");
        nonNegative(policyVersion, "policyVersion");
        snapshotChecksum = required(
                snapshotChecksum,
                "snapshotChecksum"
        );
    }

    public record ApplicationActiveRoles(
            String applicationCode,
            List<String> rootRoleIds
    ) {

        public ApplicationActiveRoles {
            applicationCode = required(
                    applicationCode,
                    "applicationCode"
            );
            rootRoleIds = List.copyOf(Objects.requireNonNull(
                    rootRoleIds,
                    "rootRoleIds"
            ));
            rootRoleIds.forEach(id -> required(id, "rootRoleIds"));
        }
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
