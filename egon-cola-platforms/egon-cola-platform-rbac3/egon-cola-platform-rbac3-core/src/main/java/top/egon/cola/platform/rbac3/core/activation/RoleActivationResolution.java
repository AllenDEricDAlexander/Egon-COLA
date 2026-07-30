package top.egon.cola.platform.rbac3.core.activation;

import java.util.List;

public record RoleActivationResolution(
        ActiveRoleSet activeRoleSet,
        ActivationAuthorizationSnapshot snapshot,
        List<String> eligibleAssignmentIds
) {

    public RoleActivationResolution {
        if (activeRoleSet == null || snapshot == null) {
            throw new IllegalArgumentException("activeRoleSet and snapshot are required");
        }
        eligibleAssignmentIds = List.copyOf(eligibleAssignmentIds);
    }
}
