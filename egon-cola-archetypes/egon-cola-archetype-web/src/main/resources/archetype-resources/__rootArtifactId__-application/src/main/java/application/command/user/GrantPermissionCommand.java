package ${package}.application.command.user;

public record GrantPermissionCommand(String requestId, String roleCode, String permissionCode) {
}
