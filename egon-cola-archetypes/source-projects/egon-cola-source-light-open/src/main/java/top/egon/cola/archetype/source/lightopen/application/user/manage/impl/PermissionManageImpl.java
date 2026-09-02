package top.egon.cola.archetype.source.lightopen.application.user.manage.impl;

import top.egon.cola.archetype.source.lightopen.application.user.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.lightopen.application.user.convertor.UserApplicationConvertor;
import top.egon.cola.archetype.source.lightopen.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.lightopen.application.user.manage.UserUseCaseException;
import top.egon.cola.archetype.source.lightopen.application.user.query.GetUserPermissionsQuery;
import top.egon.cola.archetype.source.lightopen.application.user.result.PermissionDetailResult;
import top.egon.cola.archetype.source.lightopen.application.user.result.PermissionResult;
import top.egon.cola.archetype.source.lightopen.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.lightopen.domain.user.aggregates.RolePermissionAggregate;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.Permission;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.lightopen.domain.user.exceptions.UserDomainException;
import top.egon.cola.archetype.source.lightopen.domain.user.service.PermissionDomainService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.RoleDomainService;
import top.egon.cola.archetype.source.lightopen.domain.user.event.UserEventPublisher;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserEvent;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("permissionManageImpl")
@Lazy
@RequiredArgsConstructor
@Slf4j
public class PermissionManageImpl implements PermissionManage {
    @Qualifier("permissionDomainService")
    private final PermissionDomainService<?> permissionDomainService;
    @Qualifier("roleDomainService")
    private final RoleDomainService<?> roleDomainService;
    @Qualifier("userEventPublisher")
    private final UserEventPublisher userEventPublisher;
    @Qualifier("userApplicationValidator")
    private final UserApplicationValidator applicationValidator;
    @Qualifier("userApplicationConvertor")
    private final UserApplicationConvertor convertor;

    @Override
    @Transactional
    public PermissionResult grantPermission(GrantPermissionCommand command) {
        applicationValidator.validate(command);
        Role role = roleDomainService.findByCode(new RoleCode(command.roleCode()))
                .orElseThrow(() -> new UserUseCaseException("ROLE_NOT_FOUND", "role not found"));
        Permission permission = permissionDomainService.findByCode(new PermissionCode(command.permissionCode()))
                .orElseThrow(() -> new UserUseCaseException("PERMISSION_NOT_FOUND", "permission not found"));
        try {
            RolePermissionAggregate aggregate = permissionDomainService.grantPermission(
                    new RolePermissionAggregate(role), permission);
            roleDomainService.savePermissions(aggregate);
            userEventPublisher.publish(UserEvent.permissionGranted());
            return convertor.toResult(role, permission);
        } catch (UserDomainException exception) {
            throw new UserUseCaseException(exception.getCode(), exception.getMessage(), exception);
        }
    }

    @Override
    public List<PermissionDetailResult> getByUser(GetUserPermissionsQuery query) {
        return permissionDomainService.findByUserId(new UserId(query.userId())).stream()
                .map(permission -> new PermissionDetailResult(
                        permission.code().value(), permission.name()))
                .toList();
    }
}
