package top.egon.cola.archetype.source.lightopen.application.user.command;

public record AssignRoleCommand(
        Long userId,
        String roleCode,
        String operatorId,
        String idempotencyKey) {
    public AssignRoleCommand {
        requirePositive(userId, "userId");
        requireText(roleCode, "roleCode");
    }

    private static void requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
