package top.egon.cola.archetype.source.web.adapter.user.facade.impl;

import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import top.egon.cola.archetype.source.web.application.user.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.web.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.web.application.user.query.PermissionTreeQuery;
import top.egon.cola.archetype.source.web.application.user.result.PermissionTreeResult;
import lombok.RequiredArgsConstructor;
import top.egon.cola.organization.facade.user.dto.GrantPermissionDTO;
import top.egon.cola.organization.facade.user.dto.PermissionTreeDTO;
import top.egon.cola.organization.facade.user.PermissionFacade;
import org.springframework.stereotype.Service;

@Service("permissionFacade")
@RequiredArgsConstructor
public class PermissionFacadeImpl implements PermissionFacade {
    private final PermissionManage permissionManage;

    @Override
    public void grantPermission(GrantPermissionDTO request) {
        OrganizationFacadeSupport.invoke(() -> permissionManage.grantPermission(new GrantPermissionCommand(
            OrganizationFacadeSupport.requestId(), request.roleCode(), request.permissionCode())));
    }

    @Override
    public PermissionTreeDTO getPermissionTree(Long userId) {
        PermissionTreeResult result = OrganizationFacadeSupport.invoke(
                () -> permissionManage.getPermissionTree(new PermissionTreeQuery(userId)));
        return new PermissionTreeDTO(result.userId(), result.permissionCodes());
    }
}
