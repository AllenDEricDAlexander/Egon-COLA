package top.egon.cola.archetype.source.web.application.user.command;

public record AssignRoleCommand(String requestId, Long userId, String roleCode) {
}
