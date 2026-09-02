package top.egon.cola.archetype.source.light.adapter.user.rpc;

import top.egon.cola.archetype.source.light.facade.user.UserFacade;
import top.egon.cola.archetype.source.light.facade.user.dto.AssignRoleDTO;
import top.egon.cola.archetype.source.light.facade.user.dto.CreateUserDTO;
import top.egon.cola.archetype.source.light.facade.user.dto.UserDetailDTO;
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
    public UserDetailDTO getUser(Long userId) {
        return delegate.getUser(userId);
    }
}
