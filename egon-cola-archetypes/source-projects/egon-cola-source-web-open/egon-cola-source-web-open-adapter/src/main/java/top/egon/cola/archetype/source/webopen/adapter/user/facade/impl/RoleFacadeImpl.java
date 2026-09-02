package top.egon.cola.archetype.source.webopen.adapter.user.facade.impl;

import top.egon.cola.archetype.source.webopen.adapter.facade.impl.OrganizationFacadeSupport;
import top.egon.cola.archetype.source.webopen.application.user.command.AssignRoleCommand;
import top.egon.cola.archetype.source.webopen.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.AssignRoleRequest;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.DubboRoleServiceTriple;
import top.egon.cola.archetype.source.webopen.facade.shared.v1.Empty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RoleFacadeImpl extends DubboRoleServiceTriple.RoleServiceImplBase {

    private final RoleManage roleManage;
    private final OrganizationFacadeSupport support;

    @Override
    public Empty assignRole(AssignRoleRequest request) {
        return support.invoke(() -> {
            if (request == null) {
                throw new IllegalArgumentException("assignRole request must not be null");
            }
            roleManage.assignRole(new AssignRoleCommand(
                    support.requestId(),
                    OrganizationFacadeSupport.positiveId(request.getUserId(), "userId"),
                    request.getRoleCode()));
            return Empty.getDefaultInstance();
        });
    }
}
