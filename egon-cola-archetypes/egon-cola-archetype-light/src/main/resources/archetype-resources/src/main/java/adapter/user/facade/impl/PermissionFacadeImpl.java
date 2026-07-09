package ${package}.adapter.user.facade.impl;

import ${package}.application.user.command.GrantPermissionCommand;
import ${package}.application.user.manage.PermissionManage;
import ${package}.application.user.manage.UserUseCaseException;
import ${package}.application.user.result.PermissionResult;
import ${package}.facade.user.PermissionFacade;
import ${package}.facade.user.dto.GrantPermissionDTO;
import ${package}.facade.user.dto.PermissionDTO;
import ${package}.facade.user.exceptions.UserFacadeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionFacadeImpl implements PermissionFacade {
    private final PermissionManage permissionManage;

    @Override
    public PermissionDTO grantPermission(GrantPermissionDTO request) {
        try {
            PermissionResult result = permissionManage.grantPermission(new GrantPermissionCommand(
                    request.roleCode(), request.permissionCode(), request.operatorId(), request.requestId()));
            return new PermissionDTO(result.roleCode(), result.permissionCode(), result.status());
        } catch (UserUseCaseException exception) {
            throw new UserFacadeException(exception.getCode(), exception.getMessage());
        }
    }
}
