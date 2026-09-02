package top.egon.cola.archetype.source.light.adapter.user.facade.impl;

import top.egon.cola.archetype.source.light.application.user.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.light.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.light.application.user.manage.UserUseCaseException;
import top.egon.cola.archetype.source.light.application.user.query.GetUserPermissionsQuery;
import top.egon.cola.archetype.source.light.application.user.result.PermissionResult;
import top.egon.cola.archetype.source.light.facade.user.PermissionFacade;
import top.egon.cola.archetype.source.light.facade.user.dto.GrantPermissionDTO;
import top.egon.cola.archetype.source.light.facade.user.dto.PermissionDetailDTO;
import top.egon.cola.archetype.source.light.facade.user.dto.PermissionDTO;
import top.egon.cola.archetype.source.light.facade.user.exceptions.UserFacadeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("permissionFacadeImpl")
@RequiredArgsConstructor
@Slf4j
public class PermissionFacadeImpl implements PermissionFacade {
    @Qualifier("permissionManageImpl")
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

    @Override
    public List<PermissionDetailDTO> getUserPermissions(Long userId) {
        try {
            return permissionManage.getByUser(new GetUserPermissionsQuery(userId)).stream()
                    .map(permission -> new PermissionDetailDTO(
                            permission.code(), permission.name(), List.of()))
                    .toList();
        } catch (UserUseCaseException exception) {
            throw new UserFacadeException(exception.getCode(), exception.getMessage());
        }
    }

}
