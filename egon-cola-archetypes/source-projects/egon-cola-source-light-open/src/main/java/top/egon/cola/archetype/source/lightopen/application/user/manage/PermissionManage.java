package top.egon.cola.archetype.source.lightopen.application.user.manage;

import top.egon.cola.archetype.source.lightopen.application.user.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.lightopen.application.user.query.GetUserPermissionsQuery;
import top.egon.cola.archetype.source.lightopen.application.user.result.PermissionDetailResult;
import top.egon.cola.archetype.source.lightopen.application.user.result.PermissionResult;

import java.util.List;

public interface PermissionManage {
    PermissionResult grantPermission(GrantPermissionCommand command);

    List<PermissionDetailResult> getByUser(GetUserPermissionsQuery query);
}
