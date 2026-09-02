package top.egon.cola.archetype.source.web.adapter.user.facade.impl;

import top.egon.cola.archetype.source.web.adapter.user.converter.UserAdapterConverter;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import top.egon.cola.archetype.source.web.application.user.command.CreateUserCommand;
import top.egon.cola.archetype.source.web.application.user.manage.UserManage;
import top.egon.cola.archetype.source.web.application.user.query.UserDetailQuery;
import lombok.RequiredArgsConstructor;
import top.egon.cola.organization.facade.user.dto.CreateUserDTO;
import top.egon.cola.organization.facade.user.dto.UserDetailDTO;
import top.egon.cola.organization.facade.user.UserFacade;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component("userFacade")
@Validated
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {

    private final UserManage userManage;
    private final UserAdapterConverter converter;

    @Override
    public UserDetailDTO createUser(CreateUserDTO request) {
        return OrganizationFacadeSupport.invoke(() -> converter.toDTO(userManage.createUser(
            new CreateUserCommand(OrganizationFacadeSupport.requestId(), request.name(), request.email()))));
    }

    @Override
    public UserDetailDTO getUser(Long userId) {
        return OrganizationFacadeSupport.invoke(
                () -> converter.toDTO(userManage.getUser(new UserDetailQuery(userId))));
    }
}
