package ${package}.application.user.command;

public record AssignRoleCommand(
        long userId,
        String roleCode,
        String operatorId,
        String idempotencyKey) {
    public AssignRoleCommand {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        requireText(roleCode, "roleCode");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
