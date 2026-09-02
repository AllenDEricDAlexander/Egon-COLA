package top.egon.cola.archetype.source.web.application.user.manage;

import top.egon.cola.archetype.source.web.application.user.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.web.application.user.query.PermissionTreeQuery;
import top.egon.cola.archetype.source.web.application.user.result.PermissionTreeResult;

public interface PermissionManage {
    void grantPermission(GrantPermissionCommand command);
    PermissionTreeResult getPermissionTree(PermissionTreeQuery query);
}
