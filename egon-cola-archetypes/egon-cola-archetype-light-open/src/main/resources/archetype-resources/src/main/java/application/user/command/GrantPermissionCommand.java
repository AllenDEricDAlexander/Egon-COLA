package ${package}.application.user.command;

public record GrantPermissionCommand(
        String roleCode,
        String permissionCode,
        String operatorId,
        String idempotencyKey) {
    public GrantPermissionCommand {
        requireText(roleCode, "roleCode");
        requireText(permissionCode, "permissionCode");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
