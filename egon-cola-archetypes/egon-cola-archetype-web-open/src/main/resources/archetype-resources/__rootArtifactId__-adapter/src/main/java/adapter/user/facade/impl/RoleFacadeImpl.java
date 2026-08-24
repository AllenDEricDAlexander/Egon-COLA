package ${package}.adapter.user.facade.impl;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.adapter.facade.impl.OrganizationIdBoundary;
import ${package}.application.user.command.AssignRoleCommand;
import ${package}.application.user.manage.RoleManage;
import lombok.RequiredArgsConstructor;
import top.egon.cola.organization.facade.user.dto.AssignRoleDTO;
import top.egon.cola.organization.facade.user.RoleFacade;
import org.springframework.stereotype.Service;

@Service("roleFacade")
@RequiredArgsConstructor
public class RoleFacadeImpl implements RoleFacade {
    private final RoleManage roleManage;
    private final OrganizationFacadeSupport support;

    @Override
    public void assignRole(AssignRoleDTO request) {
        support.invoke(() -> roleManage.assignRole(new AssignRoleCommand(
            support.requestId(), OrganizationIdBoundary.parse(request.userId(), "userId"), request.roleCode())));
    }
}
