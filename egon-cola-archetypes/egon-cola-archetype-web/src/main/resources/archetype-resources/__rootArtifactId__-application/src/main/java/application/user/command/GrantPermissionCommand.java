package ${package}.application.user.command;

public record GrantPermissionCommand(String requestId, String roleCode, String permissionCode) {
}
