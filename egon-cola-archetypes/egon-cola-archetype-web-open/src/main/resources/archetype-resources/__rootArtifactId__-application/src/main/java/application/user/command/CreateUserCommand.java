package ${package}.application.user.command;

public record CreateUserCommand(String requestId, String name, String email) {
}
