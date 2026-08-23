package ${package}.adapter.user.facade.impl;

import ${package}.application.user.command.AssignRoleCommand;
import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.manage.RoleManage;
import ${package}.application.user.manage.UserManage;
import ${package}.application.user.manage.UserUseCaseException;
import ${package}.application.user.query.GetUserQuery;
import ${package}.application.user.result.UserResult;
import ${package}.facade.user.UserFacade;
import ${package}.facade.user.dto.AssignRoleDTO;
import ${package}.facade.user.dto.CreateUserDTO;
import ${package}.facade.user.dto.UserDetailDTO;
import ${package}.facade.user.exceptions.UserFacadeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("userFacadeImpl")
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {
    private final UserManage userManage;
    private final RoleManage roleManage;

    @Override
    public UserDetailDTO createUser(CreateUserDTO request) {
        try {
            return toDto(userManage.create(new CreateUserCommand(
                    request.externalId(), request.name(), request.email(), request.operatorId(), request.requestId())));
        } catch (UserUseCaseException exception) {
            throw publicFailure(exception);
        }
    }

    @Override
    public UserDetailDTO assignRole(AssignRoleDTO request) {
        try {
            return toDto(roleManage.assignRole(new AssignRoleCommand(
                    request.userId(), request.roleCode(), request.operatorId(), request.requestId())));
        } catch (UserUseCaseException exception) {
            throw publicFailure(exception);
        }
    }

    @Override
    public UserDetailDTO getUser(String userId) {
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
        return new UserFacadeException(exception.getCode(), exception.getMessage());
    }
}
