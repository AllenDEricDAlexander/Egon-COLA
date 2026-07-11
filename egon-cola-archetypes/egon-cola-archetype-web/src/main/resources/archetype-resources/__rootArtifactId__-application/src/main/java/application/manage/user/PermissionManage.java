package ${package}.application.manage.user;

import ${package}.application.command.user.GrantPermissionCommand;
import ${package}.application.query.user.PermissionTreeQuery;
import ${package}.application.result.user.PermissionTreeResult;

public interface PermissionManage {
    void grantPermission(GrantPermissionCommand command);
    PermissionTreeResult getPermissionTree(PermissionTreeQuery query);
}
