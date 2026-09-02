package top.egon.cola.archetype.source.webopen.application.user.command;

public record AssignRoleCommand(String requestId, Long userId, String roleCode) {
}
