package top.egon.fable.web.adapter.facade.user;

import top.egon.fable.web.adapter.convertor.UserAdapterConverter;
import top.egon.fable.web.application.manage.user.UserManage;
import top.egon.fable.web.facade.dto.PageResponse;
import top.egon.fable.web.facade.dto.user.CreateUserRequest;
import top.egon.fable.web.facade.dto.user.UserDTO;
import top.egon.fable.web.facade.user.UserFacade;
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

    @Override
    public PageResponse<UserDTO> getUsers(int currentPage, int pageSize) {
        return userAdapterConverter.toPageResponse(userManage.getPage(currentPage, pageSize));
    }
}
