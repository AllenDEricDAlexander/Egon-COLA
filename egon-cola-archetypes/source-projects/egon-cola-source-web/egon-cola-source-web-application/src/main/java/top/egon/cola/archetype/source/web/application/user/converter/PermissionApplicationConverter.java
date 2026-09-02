package top.egon.cola.archetype.source.web.application.user.converter;

import top.egon.cola.archetype.source.web.application.user.command.GrantPermissionCommand;

public final class PermissionApplicationConverter {
    public GrantPermissionCommand toCommand(String requestId, String roleCode, String permissionCode) {
        return new GrantPermissionCommand(requestId, roleCode, permissionCode);
    }
}
