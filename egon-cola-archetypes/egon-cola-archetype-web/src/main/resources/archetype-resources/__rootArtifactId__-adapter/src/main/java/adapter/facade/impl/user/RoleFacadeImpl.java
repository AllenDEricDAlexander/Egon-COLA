package ${package}.adapter.facade.impl.user;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.application.command.user.AssignRoleCommand;
import ${package}.application.manage.user.RoleManage;
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
