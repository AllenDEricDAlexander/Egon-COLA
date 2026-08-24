#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.user.facade.impl;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.application.user.command.GrantPermissionCommand;
import ${package}.application.user.manage.PermissionManage;
import ${package}.application.user.query.PermissionTreeQuery;
import ${package}.application.user.result.PermissionTreeResult;
import ${package}.facade.organization.v1.DubboPermissionServiceTriple;
import ${package}.facade.organization.v1.GetPermissionTreeRequest;
import ${package}.facade.organization.v1.GrantPermissionRequest;
import ${package}.facade.organization.v1.PermissionTree;
import ${package}.facade.shared.v1.Empty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PermissionFacadeImpl extends DubboPermissionServiceTriple.PermissionServiceImplBase {

    private final PermissionManage permissionManage;
    private final OrganizationFacadeSupport support;

    @Override
    public Empty grantPermission(GrantPermissionRequest request) {
        return support.invoke(() -> {
            if (request == null) {
                throw new IllegalArgumentException("grantPermission request must not be null");
            }
            permissionManage.grantPermission(new GrantPermissionCommand(
                    support.requestId(), request.getRoleCode(), request.getPermissionCode()));
            return Empty.getDefaultInstance();
        });
    }

    @Override
    public PermissionTree getPermissionTree(GetPermissionTreeRequest request) {
        return support.invoke(() -> {
            if (request == null) {
                throw new IllegalArgumentException("getPermissionTree request must not be null");
            }
            PermissionTreeResult result = permissionManage.getPermissionTree(new PermissionTreeQuery(
                    OrganizationFacadeSupport.positiveId(request.getUserId(), "userId")));
            return PermissionTree.newBuilder()
                    .setUserId(result.userId())
                    .addAllPermissionCodes(result.permissionCodes())
                    .build();
        });
    }
}
