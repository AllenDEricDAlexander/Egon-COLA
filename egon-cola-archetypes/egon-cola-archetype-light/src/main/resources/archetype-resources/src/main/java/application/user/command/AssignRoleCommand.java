package ${package}.application.user.command;

public record AssignRoleCommand(
        String userId,
        String roleCode,
        String operatorId,
        String idempotencyKey) {
    public AssignRoleCommand {
        requireText(userId, "userId");
        requireText(roleCode, "roleCode");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
