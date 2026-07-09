package ${package}.adapter.user.rpc;

import ${package}.facade.user.UserFacade;
import ${package}.facade.user.dto.AssignRoleDTO;
import ${package}.facade.user.dto.CreateUserDTO;
import ${package}.facade.user.dto.UserDetailDTO;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

@DubboService(interfaceClass = UserFacade.class, version = "1.0.0", group = "user")
public class UserRpcProvider implements UserFacade {
    private final UserFacade delegate;

    public UserRpcProvider(@Qualifier("userFacadeImpl") UserFacade delegate) {
        this.delegate = delegate;
    }

    @Override
    public UserDetailDTO createUser(CreateUserDTO request) {
        return delegate.createUser(request);
    }

    @Override
    public UserDetailDTO assignRole(AssignRoleDTO request) {
        return delegate.assignRole(request);
    }

    @Override
    public UserDetailDTO getUser(String userId) {
        return delegate.getUser(userId);
    }
}
