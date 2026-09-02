package top.egon.cola.archetype.source.lightopen.application.user.manage;

import top.egon.cola.archetype.source.lightopen.application.user.command.AssignRoleCommand;
import top.egon.cola.archetype.source.lightopen.application.user.result.UserResult;

public interface RoleManage {
    UserResult assignRole(AssignRoleCommand command);
}
