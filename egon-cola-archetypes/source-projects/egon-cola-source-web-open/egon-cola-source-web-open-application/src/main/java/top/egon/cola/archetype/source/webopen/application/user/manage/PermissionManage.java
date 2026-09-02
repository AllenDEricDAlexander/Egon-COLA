package top.egon.cola.archetype.source.webopen.application.user.manage;

import top.egon.cola.archetype.source.webopen.application.user.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.webopen.application.user.query.PermissionTreeQuery;
import top.egon.cola.archetype.source.webopen.application.user.result.PermissionTreeResult;

public interface PermissionManage {
    void grantPermission(GrantPermissionCommand command);
    PermissionTreeResult getPermissionTree(PermissionTreeQuery query);
}
