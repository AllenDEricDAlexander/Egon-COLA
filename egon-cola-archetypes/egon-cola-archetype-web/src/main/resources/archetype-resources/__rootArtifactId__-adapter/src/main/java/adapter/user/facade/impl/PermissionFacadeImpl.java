package ${package}.adapter.user.facade.impl;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.application.user.command.GrantPermissionCommand;
import ${package}.application.user.manage.PermissionManage;
import ${package}.application.user.query.PermissionTreeQuery;
import ${package}.application.user.result.PermissionTreeResult;
import ${package}.facade.user.dto.GrantPermissionDTO;
import ${package}.facade.user.dto.PermissionTreeDTO;
import ${package}.facade.user.PermissionFacade;
import org.springframework.stereotype.Service;

@Service("permissionFacade")
public class PermissionFacadeImpl implements PermissionFacade {
    private final PermissionManage permissionManage;

    public PermissionFacadeImpl(PermissionManage permissionManage) { this.permissionManage = permissionManage; }

    @Override
    public void grantPermission(GrantPermissionDTO request) {
        OrganizationFacadeSupport.invoke(() -> permissionManage.grantPermission(new GrantPermissionCommand(
            OrganizationFacadeSupport.requestId(), request.roleCode(), request.permissionCode())));
    }

    @Override
    public PermissionTreeDTO getPermissionTree(String userId) {
        PermissionTreeResult result = OrganizationFacadeSupport.invoke(
                () -> permissionManage.getPermissionTree(new PermissionTreeQuery(userId)));
        return new PermissionTreeDTO(result.userId(), result.permissionCodes());
    }
}
