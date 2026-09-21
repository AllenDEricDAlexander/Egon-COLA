package top.egon.cola.archetype.source.lightopen.application.user.manage.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.archetype.source.lightopen.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.convertor.UserApplicationConvertor;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.query.GetUserPermissionsQuery;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.result.PermissionDetailResult;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.result.PermissionResult;
import top.egon.cola.archetype.source.lightopen.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.lightopen.common.exception.UserDomainException;
import top.egon.cola.archetype.source.lightopen.common.exception.UserUseCaseException;
import top.egon.cola.archetype.source.lightopen.domain.user.aggregates.RolePermissionAggregate;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.Permission;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.lightopen.domain.user.service.PermissionDomainService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.RoleDomainService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserEventService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserIdempotencyService;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserEvent;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserId;

import java.util.List;

@Service("permissionManageImpl")
@Lazy
@RequiredArgsConstructor
@Slf4j
public class PermissionManageImpl implements PermissionManage {
    @Qualifier("permissionDomainService")
    private final PermissionDomainService permissionDomainService;
    @Qualifier("roleDomainService")
    private final RoleDomainService roleDomainService;
    @Qualifier("userEventService")
    private final UserEventService userEventService;
    @Qualifier("userIdempotencyService")
    private final UserIdempotencyService userIdempotencyService;
    @Qualifier("userApplicationValidator")
    private final UserApplicationValidator applicationValidator;
    @Qualifier("userApplicationConvertorImpl")
    private final UserApplicationConvertor convertor;

    @Override
    @Transactional
    public PermissionResult grantPermission(GrantPermissionCommand command) {
        applicationValidator.validate(command);
        if (!userIdempotencyService.claim(command.idempotencyKey())) {
            throw new UserUseCaseException("DUPLICATE_REQUEST", "request was already processed");
        }
        Role role = roleDomainService.findByCode(new RoleCode(command.roleCode()))
                .orElseThrow(() -> new UserUseCaseException("ROLE_NOT_FOUND", "role not found"));
        Permission permission = permissionDomainService.findByCode(new PermissionCode(command.permissionCode()))
                .orElseThrow(() -> new UserUseCaseException("PERMISSION_NOT_FOUND", "permission not found"));
        try {
            RolePermissionAggregate aggregate = permissionDomainService.grantPermission(
                    new RolePermissionAggregate(role), permission);
            roleDomainService.savePermissions(aggregate);
            userEventService.publish(UserEvent.permissionGranted());
            return convertor.toPermissionResult(role, permission);
        } catch (UserDomainException exception) {
            throw new UserUseCaseException(exception.getStatus(), exception.getMessage(), exception);
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
