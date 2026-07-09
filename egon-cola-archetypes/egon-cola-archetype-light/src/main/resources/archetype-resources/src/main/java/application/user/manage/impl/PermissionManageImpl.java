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
import ${package}.domain.user.repos.PermissionRepository;
import ${package}.domain.user.repos.RoleRepository;
import ${package}.domain.user.service.PermissionDomainService;
import ${package}.domain.user.service.UserEventPublisher;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserEvent;
import ${package}.domain.user.vos.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Lazy
@RequiredArgsConstructor
public class PermissionManageImpl implements PermissionManage {
    @Qualifier("permissionDomainService")
    private final PermissionDomainService permissionDomainService;
    @Qualifier("roleRepository")
    private final RoleRepository roleRepository;
    @Qualifier("permissionRepository")
    private final PermissionRepository permissionRepository;
    @Qualifier("userEventPublisher")
    private final UserEventPublisher userEventPublisher;
    private final UserApplicationValidator applicationValidator;
    private final UserApplicationConvertor convertor;

    @Override
    @Transactional
    public PermissionResult grantPermission(GrantPermissionCommand command) {
        applicationValidator.validate(command);
        Role role = roleRepository.findByCode(new RoleCode(command.roleCode()))
                .orElseThrow(() -> new UserUseCaseException("ROLE_NOT_FOUND", "role not found"));
        Permission permission = permissionRepository.findByCode(new PermissionCode(command.permissionCode()))
                .orElseThrow(() -> new UserUseCaseException("PERMISSION_NOT_FOUND", "permission not found"));
        try {
            RolePermissionAggregate aggregate = permissionDomainService.grantPermission(
                    new RolePermissionAggregate(role), permission);
            roleRepository.savePermissions(aggregate);
            userEventPublisher.publish(UserEvent.permissionGranted(role.code().value()));
            return convertor.toResult(role, permission);
        } catch (UserDomainException exception) {
            throw new UserUseCaseException(exception.getCode(), exception.getMessage(), exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDetailResult> getByUser(GetUserPermissionsQuery query) {
        return permissionRepository.findByUserId(new UserId(query.userId())).stream()
                .map(permission -> new PermissionDetailResult(
                        permission.code().value(), permission.name()))
                .toList();
    }
}
