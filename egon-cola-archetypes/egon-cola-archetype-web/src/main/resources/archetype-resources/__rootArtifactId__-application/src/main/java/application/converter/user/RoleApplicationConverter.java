package ${package}.application.converter.user;

import ${package}.application.command.user.AssignRoleCommand;

public final class RoleApplicationConverter {
    public AssignRoleCommand toCommand(String requestId, String userId, String roleCode) {
        return new AssignRoleCommand(requestId, userId, roleCode);
    }
}
