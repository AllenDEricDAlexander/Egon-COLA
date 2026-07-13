package ${package}.adapter.user.facade.impl;

import ${package}.adapter.user.converter.UserAdapterConverter;
import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.manage.UserManage;
import ${package}.application.user.query.UserDetailQuery;
import ${package}.facade.user.dto.CreateUserDTO;
import ${package}.facade.user.dto.UserDetailDTO;
import ${package}.facade.user.UserFacade;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component("userFacade")
@Validated
public class UserFacadeImpl implements UserFacade {

    private final UserManage userManage;
    private final UserAdapterConverter converter;

    public UserFacadeImpl(UserManage userManage, UserAdapterConverter converter) {
        this.userManage = userManage;
        this.converter = converter;
    }

    @Override
    public UserDetailDTO createUser(CreateUserDTO request) {
        return OrganizationFacadeSupport.invoke(() -> converter.toDTO(userManage.createUser(
            new CreateUserCommand(OrganizationFacadeSupport.requestId(), request.name(), request.email()))));
    }

    @Override
    public UserDetailDTO getUser(String userId) {
        return OrganizationFacadeSupport.invoke(
                () -> converter.toDTO(userManage.getUser(new UserDetailQuery(userId))));
    }
}
