package ${package}.application.user.command;

public record AssignRoleCommand(String requestId, Long userId, String roleCode) {
}
