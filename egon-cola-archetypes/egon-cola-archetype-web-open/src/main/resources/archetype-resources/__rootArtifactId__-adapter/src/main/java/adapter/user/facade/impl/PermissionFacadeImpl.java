package ${package}.adapter.user.facade.impl;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.adapter.facade.impl.OrganizationIdBoundary;
import ${package}.application.user.command.GrantPermissionCommand;
import ${package}.application.user.manage.PermissionManage;
import ${package}.application.user.query.PermissionTreeQuery;
import ${package}.application.user.result.PermissionTreeResult;
import lombok.RequiredArgsConstructor;
import top.egon.cola.organization.facade.user.dto.GrantPermissionDTO;
import top.egon.cola.organization.facade.user.dto.PermissionTreeDTO;
import top.egon.cola.organization.facade.user.PermissionFacade;
import org.springframework.stereotype.Service;

@Service("permissionFacade")
@RequiredArgsConstructor
public class PermissionFacadeImpl implements PermissionFacade {
    private final PermissionManage permissionManage;
    private final OrganizationFacadeSupport support;

    @Override
    public void grantPermission(GrantPermissionDTO request) {
        support.invoke(() -> permissionManage.grantPermission(new GrantPermissionCommand(
            support.requestId(), request.roleCode(), request.permissionCode())));
    }

    @Override
    public PermissionTreeDTO getPermissionTree(String userId) {
        PermissionTreeResult result = support.invoke(
                () -> permissionManage.getPermissionTree(new PermissionTreeQuery(
                    OrganizationIdBoundary.parse(userId, "userId"))));
        return new PermissionTreeDTO(Long.toString(result.userId()), result.permissionCodes());
    }
}
