package top.egon.cola.archetype.source.web.application.user.manage;

import top.egon.cola.archetype.source.web.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.web.application.user.pojo.query.PermissionTreeQuery;
import top.egon.cola.archetype.source.web.application.user.pojo.result.PermissionTreeResult;

public interface PermissionManage {
    void grantPermission(GrantPermissionCommand command);
    PermissionTreeResult getPermissionTree(PermissionTreeQuery query);
}
