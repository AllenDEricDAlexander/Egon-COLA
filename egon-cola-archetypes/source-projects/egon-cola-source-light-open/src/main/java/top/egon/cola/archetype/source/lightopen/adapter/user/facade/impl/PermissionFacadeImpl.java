package top.egon.cola.archetype.source.lightopen.adapter.user.facade.impl;

import top.egon.cola.archetype.source.lightopen.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.lightopen.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.lightopen.common.exception.UserUseCaseException;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.query.GetUserPermissionsQuery;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.result.PermissionResult;
import top.egon.cola.archetype.source.lightopen.facade.user.PermissionFacade;
import top.egon.cola.archetype.source.lightopen.facade.user.dto.GrantPermissionDTO;
import top.egon.cola.archetype.source.lightopen.facade.user.dto.PermissionDetailDTO;
import top.egon.cola.archetype.source.lightopen.facade.user.dto.PermissionDTO;
import top.egon.cola.archetype.source.lightopen.common.exception.UserFacadeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import top.egon.cola.component.common.core.validation.ValidationUtils;

@Component("permissionFacadeImpl")
@RequiredArgsConstructor
@Slf4j
public class PermissionFacadeImpl implements PermissionFacade {
    @Qualifier("permissionManageImpl")
    private final PermissionManage permissionManage;
    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    public PermissionDTO grantPermission(GrantPermissionDTO request) {
        validationUtils.validate(request);
        try {
            PermissionResult result = permissionManage.grantPermission(new GrantPermissionCommand(
                    request.roleCode(), request.permissionCode(), request.operatorId(), request.requestId()));
            return new PermissionDTO(result.roleCode(), result.permissionCode(), result.status());
        } catch (UserUseCaseException exception) {
            throw new UserFacadeException(exception.getStatus(), exception.getMessage());
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
            throw new UserFacadeException(exception.getStatus(), exception.getMessage());
        }
    }

}
