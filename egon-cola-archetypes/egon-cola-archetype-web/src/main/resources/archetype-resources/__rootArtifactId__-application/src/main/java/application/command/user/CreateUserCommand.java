package ${package}.application.command.user;

public record CreateUserCommand(String requestId, String name, String email) {
}
