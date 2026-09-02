package top.egon.cola.archetype.source.web.application.user.command;

public record CreateUserCommand(String requestId, String name, String email) {
}
