package ${package}.application.converter.user;

import ${package}.application.command.user.GrantPermissionCommand;

public final class PermissionApplicationConverter {
    public GrantPermissionCommand toCommand(String requestId, String roleCode, String permissionCode) {
        return new GrantPermissionCommand(requestId, roleCode, permissionCode);
    }
}
