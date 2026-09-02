package top.egon.cola.archetype.source.webopen.application.user.manage;

import top.egon.cola.archetype.source.webopen.application.user.command.AssignRoleCommand;

public interface RoleManage {
    void assignRole(AssignRoleCommand command);
}
