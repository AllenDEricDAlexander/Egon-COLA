package ${package}.application.command.user;

public record AssignRoleCommand(String requestId, String userId, String roleCode) {
}
