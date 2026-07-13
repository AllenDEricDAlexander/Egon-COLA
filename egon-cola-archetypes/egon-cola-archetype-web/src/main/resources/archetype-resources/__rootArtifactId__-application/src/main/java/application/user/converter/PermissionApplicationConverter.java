package ${package}.application.user.converter;

import ${package}.application.user.command.GrantPermissionCommand;

public final class PermissionApplicationConverter {
    public GrantPermissionCommand toCommand(String requestId, String roleCode, String permissionCode) {
        return new GrantPermissionCommand(requestId, roleCode, permissionCode);
    }
}
