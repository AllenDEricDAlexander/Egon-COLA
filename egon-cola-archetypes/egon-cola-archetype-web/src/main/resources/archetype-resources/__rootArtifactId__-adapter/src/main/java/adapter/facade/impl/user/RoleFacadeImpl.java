package ${package}.adapter.facade.impl.user;

import ${package}.application.command.user.AssignRoleCommand;
import ${package}.application.manage.user.RoleManage;
import ${package}.facade.dto.user.AssignRoleDTO;
import ${package}.facade.user.RoleFacade;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("roleFacade")
public class RoleFacadeImpl implements RoleFacade {
    private final RoleManage roleManage;

    public RoleFacadeImpl(RoleManage roleManage) { this.roleManage = roleManage; }

    @Override
    public void assignRole(AssignRoleDTO request) {
        roleManage.assignRole(new AssignRoleCommand(
            UUID.randomUUID().toString(), request.userId(), request.roleCode()));
    }
}
