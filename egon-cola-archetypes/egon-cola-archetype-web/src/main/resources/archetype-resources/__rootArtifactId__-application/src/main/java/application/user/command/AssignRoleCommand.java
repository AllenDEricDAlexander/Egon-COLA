package ${package}.application.user.command;

public record AssignRoleCommand(String requestId, String userId, String roleCode) {
}
