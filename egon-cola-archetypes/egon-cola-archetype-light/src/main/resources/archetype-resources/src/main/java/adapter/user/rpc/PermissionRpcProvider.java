package ${package}.adapter.user.rpc;

import ${package}.facade.user.PermissionFacade;
import ${package}.facade.user.dto.GrantPermissionDTO;
import ${package}.facade.user.dto.PermissionDTO;
import ${package}.facade.user.dto.PermissionDetailDTO;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

@DubboService(interfaceClass = PermissionFacade.class, version = "1.0.0", group = "user")
public class PermissionRpcProvider implements PermissionFacade {
    private final PermissionFacade delegate;

    public PermissionRpcProvider(@Qualifier("permissionFacadeImpl") PermissionFacade delegate) {
        this.delegate = delegate;
    }

    @Override
    public PermissionDTO grantPermission(GrantPermissionDTO request) {
        return delegate.grantPermission(request);
    }

    @Override
    public List<PermissionDetailDTO> getUserPermissions(String userId) {
        return delegate.getUserPermissions(userId);
    }
}
