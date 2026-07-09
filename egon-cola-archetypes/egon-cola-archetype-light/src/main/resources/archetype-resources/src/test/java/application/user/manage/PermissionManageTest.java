package ${package}.application.user.manage;

import ${package}.application.user.command.GrantPermissionCommand;
import ${package}.application.user.convertor.UserApplicationConvertor;
import ${package}.application.user.manage.impl.PermissionManageImpl;
import ${package}.application.user.result.PermissionResult;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.domain.user.aggregates.RolePermissionAggregate;
import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.enums.PermissionStatus;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.exceptions.UserDomainException;
import ${package}.domain.user.repos.PermissionRepository;
import ${package}.domain.user.repos.RoleRepository;
import ${package}.domain.user.service.PermissionDomainService;
import ${package}.domain.user.service.UserEventPublisher;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.RoleCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionManageTest {
    @Mock PermissionDomainService permissionDomainService;
    @Mock RoleRepository roleRepository;
    @Mock PermissionRepository permissionRepository;
    @Mock UserEventPublisher userEventPublisher;
    @Mock UserApplicationValidator applicationValidator;
    @Mock UserApplicationConvertor convertor;
    @InjectMocks PermissionManageImpl manage;

    @Test
    void grants_permission_and_persists_aggregate() {
        Role role = role(RoleStatus.ACTIVE);
        Permission permission = permission(PermissionStatus.ACTIVE);
        RolePermissionAggregate aggregate = new RolePermissionAggregate(role);
        aggregate.grant(permission);
        when(roleRepository.findByCode(new RoleCode("teacher"))).thenReturn(Optional.of(role));
        when(permissionRepository.findByCode(new PermissionCode("course:read")))
                .thenReturn(Optional.of(permission));
        when(permissionDomainService.grantPermission(any(), any())).thenReturn(aggregate);
        when(convertor.toResult(role, permission))
                .thenReturn(new PermissionResult("teacher", "course:read", "ACTIVE"));

        PermissionResult result = manage.grantPermission(command());

        assertEquals("course:read", result.permissionCode());
        verify(roleRepository).savePermissions(aggregate);
        verify(userEventPublisher).publish(any());
    }

    @Test
    void translates_inactive_permission_failure() {
        Role role = role(RoleStatus.ACTIVE);
        Permission permission = permission(PermissionStatus.DISABLED);
        when(roleRepository.findByCode(new RoleCode("teacher"))).thenReturn(Optional.of(role));
        when(permissionRepository.findByCode(new PermissionCode("course:read")))
                .thenReturn(Optional.of(permission));
        when(permissionDomainService.grantPermission(any(), any()))
                .thenThrow(new UserDomainException("PERMISSION_NOT_ACTIVE", "permission must be active"));

        UserUseCaseException error = assertThrows(
                UserUseCaseException.class, () -> manage.grantPermission(command()));

        assertEquals("PERMISSION_NOT_ACTIVE", error.getCode());
    }

    private GrantPermissionCommand command() {
        return new GrantPermissionCommand("teacher", "course:read", "operator-1", "request-1");
    }

    private Role role(RoleStatus status) {
        return new Role(new RoleCode("teacher"), "Teacher", status);
    }

    private Permission permission(PermissionStatus status) {
        return new Permission(new PermissionCode("course:read"), "Read courses", status);
    }
}
