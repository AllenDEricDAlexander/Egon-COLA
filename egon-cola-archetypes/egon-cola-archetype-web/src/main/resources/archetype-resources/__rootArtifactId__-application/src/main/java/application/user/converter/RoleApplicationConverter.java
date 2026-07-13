package ${package}.application.user.converter;

import ${package}.application.user.command.AssignRoleCommand;

public final class RoleApplicationConverter {
    public AssignRoleCommand toCommand(String requestId, String userId, String roleCode) {
        return new AssignRoleCommand(requestId, userId, roleCode);
    }
}
