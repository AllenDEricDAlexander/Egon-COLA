package ${package}.adapter.facade.impl.user;

import ${package}.application.command.user.GrantPermissionCommand;
import ${package}.application.manage.user.PermissionManage;
import ${package}.application.query.user.PermissionTreeQuery;
import ${package}.application.result.user.PermissionTreeResult;
import ${package}.facade.dto.user.GrantPermissionDTO;
import ${package}.facade.dto.user.PermissionTreeDTO;
import ${package}.facade.user.PermissionFacade;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("permissionFacade")
public class PermissionFacadeImpl implements PermissionFacade {
    private final PermissionManage permissionManage;

    public PermissionFacadeImpl(PermissionManage permissionManage) { this.permissionManage = permissionManage; }

    @Override
    public void grantPermission(GrantPermissionDTO request) {
        permissionManage.grantPermission(new GrantPermissionCommand(
            UUID.randomUUID().toString(), request.roleCode(), request.permissionCode()));
    }

    @Override
    public PermissionTreeDTO getPermissionTree(String userId) {
        PermissionTreeResult result = permissionManage.getPermissionTree(new PermissionTreeQuery(userId));
        return new PermissionTreeDTO(result.userId(), result.permissionCodes());
    }
}
