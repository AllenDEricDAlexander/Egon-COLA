package top.egon.cola.archetype.source.web.application.user.converter;

import top.egon.cola.archetype.source.web.application.user.command.AssignRoleCommand;

public final class RoleApplicationConverter {
    public AssignRoleCommand toCommand(String requestId, Long userId, String roleCode) {
        return new AssignRoleCommand(requestId, userId, roleCode);
    }
}
