package ${package}.adapter.user.rpc;

import ${package}.facade.user.UserFacade;
import ${package}.facade.user.dto.AssignRoleDTO;
import ${package}.facade.user.dto.CreateUserDTO;
import ${package}.facade.user.dto.UserDetailDTO;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

@DubboService(interfaceClass = UserFacade.class, version = "1.0.0", group = "user")
@RequiredArgsConstructor
public class UserRpcProvider implements UserFacade {
    @Qualifier("userFacadeImpl")
    private final UserFacade delegate;

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
