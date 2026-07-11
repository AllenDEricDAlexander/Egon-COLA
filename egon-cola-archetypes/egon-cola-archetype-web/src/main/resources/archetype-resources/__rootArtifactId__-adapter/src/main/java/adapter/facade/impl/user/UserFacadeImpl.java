package ${package}.adapter.facade.impl.user;

import ${package}.adapter.converter.UserAdapterConverter;
import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.application.command.user.CreateUserCommand;
import ${package}.application.manage.user.UserManage;
import ${package}.application.query.user.UserDetailQuery;
import ${package}.facade.dto.user.CreateUserDTO;
import ${package}.facade.dto.user.UserDetailDTO;
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
