package top.egon.cola.archetype.source.light.application.user.manage;

import top.egon.cola.archetype.source.light.application.user.command.AssignRoleCommand;
import top.egon.cola.archetype.source.light.application.user.result.UserResult;

public interface RoleManage {
    UserResult assignRole(AssignRoleCommand command);
}
