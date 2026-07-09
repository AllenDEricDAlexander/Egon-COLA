package ${package}.application.user.command;

public record CreateUserCommand(
        String externalId,
        String name,
        String email,
        String operatorId,
        String idempotencyKey) {
    public CreateUserCommand {
        requireText(externalId, "externalId");
        requireText(name, "name");
        requireText(email, "email");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
