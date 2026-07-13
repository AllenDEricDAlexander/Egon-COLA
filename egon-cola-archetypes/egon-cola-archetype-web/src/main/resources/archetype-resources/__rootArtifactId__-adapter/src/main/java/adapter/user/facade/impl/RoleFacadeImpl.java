package ${package}.adapter.user.facade.impl;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.application.user.command.AssignRoleCommand;
import ${package}.application.user.manage.RoleManage;
import ${package}.facade.user.dto.AssignRoleDTO;
import ${package}.facade.user.RoleFacade;
import org.springframework.stereotype.Service;

@Service("roleFacade")
public class RoleFacadeImpl implements RoleFacade {
    private final RoleManage roleManage;

    public RoleFacadeImpl(RoleManage roleManage) { this.roleManage = roleManage; }

    @Override
    public void assignRole(AssignRoleDTO request) {
        OrganizationFacadeSupport.invoke(() -> roleManage.assignRole(new AssignRoleCommand(
            OrganizationFacadeSupport.requestId(), request.userId(), request.roleCode())));
    }
}
