package ${package}.application.user.manage.impl;

import ${package}.application.user.command.GrantPermissionCommand;
import ${package}.application.user.convertor.UserApplicationConvertor;
import ${package}.application.user.manage.PermissionManage;
import ${package}.application.user.manage.UserUseCaseException;
import ${package}.application.user.query.GetUserPermissionsQuery;
import ${package}.application.user.result.PermissionDetailResult;
import ${package}.application.user.result.PermissionResult;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.domain.user.aggregates.RolePermissionAggregate;
import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.exceptions.UserDomainException;
import ${package}.domain.user.service.PermissionDomainService;
import ${package}.domain.user.service.RoleDomainService;
import ${package}.domain.user.event.UserEventPublisher;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserEvent;
import ${package}.domain.user.vos.UserId;
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
