package ${package}.adapter.facade.user;

import ${package}.adapter.convertor.UserAdapterConverter;
import ${package}.application.manage.user.UserManage;
import ${package}.facade.dto.user.CreateUserRequest;
import ${package}.facade.dto.user.UserDTO;
import ${package}.facade.user.UserFacade;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class UserFacadeImpl implements UserFacade {
    private final UserManage userManage;

    public UserFacadeImpl(UserManage userManage) {
        this.userManage = userManage;
    }

    @Override
    public UserDTO createUser(CreateUserRequest request) {
        return UserAdapterConverter.toDto(userManage.create(request.name(), request.email()));
    }

    @Override
    public UserDTO getUser(String userId) {
        return UserAdapterConverter.toDto(userManage.getById(userId));
    }
}
