package ${package}.application.user.manage;

import ${package}.application.user.command.GrantPermissionCommand;
import ${package}.application.user.result.PermissionResult;

public interface PermissionManage {
    PermissionResult grantPermission(GrantPermissionCommand command);
}
