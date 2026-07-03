package ${package}.adapter.facade.user;

import ${package}.adapter.convertor.UserAdapterConverter;
import ${package}.application.manage.user.UserManage;
import ${package}.facade.dto.user.CreateUserRequest;
import ${package}.facade.dto.user.UserDTO;
import ${package}.facade.user.UserFacade;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;

@DubboService(
        interfaceClass = UserFacade.class,
        version = "1.0.0",
        group = "user"
)
@Validated
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {
    @Qualifier("userManage")
    private final UserManage userManage;

    @Qualifier("userAdapterConverter")
    private final UserAdapterConverter userAdapterConverter;

    @Override
    public UserDTO createUser(CreateUserRequest request) {
        return userAdapterConverter.toDto(userManage.create(request.name(), request.email()));
    }

    @Override
    public UserDTO getUser(String userId) {
        return userAdapterConverter.toDto(userManage.getById(userId));
    }
}
