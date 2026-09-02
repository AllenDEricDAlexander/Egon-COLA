package top.egon.cola.archetype.source.webopen.adapter.user.facade.impl;

import top.egon.cola.archetype.source.webopen.adapter.facade.impl.OrganizationFacadeSupport;
import top.egon.cola.archetype.source.webopen.application.user.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.webopen.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.webopen.application.user.query.PermissionTreeQuery;
import top.egon.cola.archetype.source.webopen.application.user.result.PermissionTreeResult;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.DubboPermissionServiceTriple;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.GetPermissionTreeRequest;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.GrantPermissionRequest;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.PermissionTree;
import top.egon.cola.archetype.source.webopen.facade.shared.v1.Empty;
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
