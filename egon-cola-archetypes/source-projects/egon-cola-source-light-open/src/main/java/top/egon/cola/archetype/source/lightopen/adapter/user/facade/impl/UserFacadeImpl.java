package top.egon.cola.archetype.source.lightopen.adapter.user.facade.impl;

import top.egon.cola.archetype.source.lightopen.application.user.pojo.command.AssignRoleCommand;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.lightopen.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.lightopen.application.user.manage.UserManage;
import top.egon.cola.archetype.source.lightopen.common.exception.UserUseCaseException;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.query.GetUserQuery;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.result.UserResult;
import top.egon.cola.archetype.source.lightopen.facade.user.UserFacade;
import top.egon.cola.archetype.source.lightopen.facade.user.dto.AssignRoleDTO;
import top.egon.cola.archetype.source.lightopen.facade.user.dto.CreateUserDTO;
import top.egon.cola.archetype.source.lightopen.facade.user.dto.UserDetailDTO;
import top.egon.cola.archetype.source.lightopen.common.exception.UserFacadeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.validation.ValidationUtils;

@Component("userFacadeImpl")
@RequiredArgsConstructor
@Slf4j
public class UserFacadeImpl implements UserFacade {
    @Qualifier("userManageImpl")
    private final UserManage userManage;
    @Qualifier("roleManageImpl")
    private final RoleManage roleManage;
    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    public UserDetailDTO createUser(CreateUserDTO request) {
        validationUtils.validate(request);
        try {
            return toDto(userManage.create(new CreateUserCommand(
                    request.externalId(), request.name(), request.email(), request.operatorId(), request.requestId())));
        } catch (UserUseCaseException exception) {
            throw publicFailure(exception);
        }
    }

    @Override
    public UserDetailDTO assignRole(AssignRoleDTO request) {
        validationUtils.validate(request);
        try {
            return toDto(roleManage.assignRole(new AssignRoleCommand(
                    request.userId(), request.roleCode(), request.operatorId(), request.requestId())));
        } catch (UserUseCaseException exception) {
            throw publicFailure(exception);
        }
    }

    @Override
    public UserDetailDTO getUser(Long userId) {
        try {
            return toDto(userManage.get(new GetUserQuery(userId)));
        } catch (UserUseCaseException exception) {
            throw publicFailure(exception);
        }
    }

    private static UserDetailDTO toDto(UserResult result) {
        return new UserDetailDTO(result.id(), result.name(), result.email(), result.status());
    }

    private static UserFacadeException publicFailure(UserUseCaseException exception) {
        return new UserFacadeException(exception.getStatus(), exception.getMessage());
    }
}
