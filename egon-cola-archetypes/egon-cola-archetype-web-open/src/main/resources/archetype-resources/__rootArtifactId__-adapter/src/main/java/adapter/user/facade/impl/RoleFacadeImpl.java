#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.user.facade.impl;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.application.user.command.AssignRoleCommand;
import ${package}.application.user.manage.RoleManage;
import ${package}.facade.organization.v1.AssignRoleRequest;
import ${package}.facade.organization.v1.DubboRoleServiceTriple;
import ${package}.facade.shared.v1.Empty;
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
