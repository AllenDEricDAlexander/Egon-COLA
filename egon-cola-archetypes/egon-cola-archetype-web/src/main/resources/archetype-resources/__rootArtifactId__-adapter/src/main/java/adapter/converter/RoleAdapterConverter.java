package ${package}.adapter.converter;

import ${package}.adapter.dto.user.AssignRoleRequest;
import ${package}.application.user.command.AssignRoleCommand;
import org.springframework.stereotype.Component;

@Component("roleAdapterConverter")
public final class RoleAdapterConverter {
    public AssignRoleCommand toCommand(String requestId, String userId, AssignRoleRequest request) {
        return new AssignRoleCommand(requestId, userId, request.roleCode());
    }
}
