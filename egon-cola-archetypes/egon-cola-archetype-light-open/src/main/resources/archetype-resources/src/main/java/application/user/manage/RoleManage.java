package ${package}.application.user.manage;

import ${package}.application.user.command.AssignRoleCommand;
import ${package}.application.user.result.UserResult;

public interface RoleManage {
    UserResult assignRole(AssignRoleCommand command);
}
