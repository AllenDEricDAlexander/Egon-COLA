package top.egon.cola.archetype.source.web.adapter.user.facade.impl;

import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import top.egon.cola.archetype.source.web.application.user.command.AssignRoleCommand;
import top.egon.cola.archetype.source.web.application.user.manage.RoleManage;
import lombok.RequiredArgsConstructor;
import top.egon.cola.organization.facade.user.dto.AssignRoleDTO;
import top.egon.cola.organization.facade.user.RoleFacade;
import org.springframework.stereotype.Service;

@Service("roleFacade")
@RequiredArgsConstructor
public class RoleFacadeImpl implements RoleFacade {
    private final RoleManage roleManage;

    @Override
    public void assignRole(AssignRoleDTO request) {
        OrganizationFacadeSupport.invoke(() -> roleManage.assignRole(new AssignRoleCommand(
            OrganizationFacadeSupport.requestId(), request.userId(), request.roleCode())));
    }
}
