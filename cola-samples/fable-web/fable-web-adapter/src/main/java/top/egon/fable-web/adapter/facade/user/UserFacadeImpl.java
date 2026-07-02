package top.egon.fable-web.adapter.facade.user;

import top.egon.fable-web.adapter.convertor.UserAdapterConverter;
import top.egon.fable-web.application.manage.user.UserManage;
import top.egon.fable-web.facade.dto.user.CreateUserRequest;
import top.egon.fable-web.facade.dto.user.UserDTO;
import top.egon.fable-web.facade.user.UserFacade;
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
