package top.egon.cola.archetype.source.web.application.user.manage;

import top.egon.cola.archetype.source.web.application.user.command.AssignRoleCommand;

public interface RoleManage {
    void assignRole(AssignRoleCommand command);
}
