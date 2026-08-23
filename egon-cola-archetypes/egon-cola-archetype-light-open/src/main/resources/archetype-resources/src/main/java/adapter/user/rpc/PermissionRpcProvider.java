package ${package}.adapter.user.rpc;

import ${package}.facade.user.PermissionFacade;
import ${package}.facade.user.dto.GrantPermissionDTO;
import ${package}.facade.user.dto.PermissionDTO;
import ${package}.facade.user.dto.PermissionDetailDTO;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

@DubboService(interfaceClass = PermissionFacade.class, version = "1.0.0", group = "user")
@RequiredArgsConstructor
public class PermissionRpcProvider implements PermissionFacade {
    @Qualifier("permissionFacadeImpl")
    private final PermissionFacade delegate;

    @Override
    public PermissionDTO grantPermission(GrantPermissionDTO request) {
        return delegate.grantPermission(request);
    }

    @Override
    public List<PermissionDetailDTO> getUserPermissions(String userId) {
        return delegate.getUserPermissions(userId);
    }
}
