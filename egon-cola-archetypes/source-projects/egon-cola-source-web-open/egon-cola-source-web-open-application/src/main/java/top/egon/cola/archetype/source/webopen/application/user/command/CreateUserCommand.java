package top.egon.cola.archetype.source.webopen.application.user.command;

public record CreateUserCommand(String requestId, String name, String email) {
}
