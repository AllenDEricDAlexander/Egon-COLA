package ${package}.application.user.manage;

import ${package}.application.user.command.GrantPermissionCommand;
import ${package}.application.user.query.GetUserPermissionsQuery;
import ${package}.application.user.result.PermissionDetailResult;
import ${package}.application.user.result.PermissionResult;

import java.util.List;

public interface PermissionManage {
    PermissionResult grantPermission(GrantPermissionCommand command);

    List<PermissionDetailResult> getByUser(GetUserPermissionsQuery query);
}
