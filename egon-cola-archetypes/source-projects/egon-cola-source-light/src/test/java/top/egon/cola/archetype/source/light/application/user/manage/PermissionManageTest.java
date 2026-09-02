package top.egon.cola.archetype.source.light.application.user.manage;

import top.egon.cola.archetype.source.light.application.user.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.light.application.user.convertor.UserApplicationConvertor;
import top.egon.cola.archetype.source.light.application.user.manage.impl.PermissionManageImpl;
import top.egon.cola.archetype.source.light.application.user.query.GetUserPermissionsQuery;
import top.egon.cola.archetype.source.light.application.user.result.PermissionDetailResult;
import top.egon.cola.archetype.source.light.application.user.result.PermissionResult;
import top.egon.cola.archetype.source.light.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.light.domain.user.entities.Permission;
import top.egon.cola.archetype.source.light.domain.user.entities.Role;
import top.egon.cola.archetype.source.light.domain.user.enums.PermissionStatus;
import top.egon.cola.archetype.source.light.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.light.domain.user.event.UserEventPublisher;
import top.egon.cola.archetype.source.light.domain.user.exceptions.UserDomainException;
import top.egon.cola.archetype.source.light.domain.user.service.PermissionDomainService;
import top.egon.cola.archetype.source.light.domain.user.service.RoleDomainService;
import top.egon.cola.archetype.source.light.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.light.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.po.PermissionPO;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.po.RolePO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionManageTest {
    @Mock PermissionDomainService<PermissionPO> permissionDomainService;
    @Mock RoleDomainService<RolePO> roleDomainService;
    @Mock UserEventPublisher userEventPublisher;
    @Mock UserApplicationValidator applicationValidator;
    @Mock UserApplicationConvertor convertor;
    @InjectMocks PermissionManageImpl manage;

    @Test
    void grants_permission_and_persists_aggregate() {
        Role role = role(RoleStatus.ACTIVE);
        Permission permission = permission(PermissionStatus.ACTIVE);
        when(roleDomainService.findByCode(new RoleCode("teacher"))).thenReturn(Optional.of(role));
        when(permissionDomainService.findByCode(new PermissionCode("course:read")))
                .thenReturn(Optional.of(permission));
        when(permissionDomainService.grantPermission(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(convertor.toResult(role, permission)).thenReturn(new PermissionResult("teacher", "course:read", "ACTIVE"));
        PermissionResult result = manage.grantPermission(new GrantPermissionCommand(
                "teacher", "course:read", "operator-1", "request-1"));
        assertEquals("course:read", result.permissionCode());
        verify(roleDomainService).savePermissions(any());
        verify(userEventPublisher).publish(any());
    }

    @Test
    void translates_inactive_permission_failure() {
        Role role = role(RoleStatus.ACTIVE);
        Permission permission = permission(PermissionStatus.DISABLED);
        when(roleDomainService.findByCode(new RoleCode("teacher"))).thenReturn(Optional.of(role));
        when(permissionDomainService.findByCode(new PermissionCode("course:read")))
                .thenReturn(Optional.of(permission));
        when(permissionDomainService.grantPermission(any(), any()))
                .thenThrow(new UserDomainException("PERMISSION_NOT_ACTIVE", "permission must be active"));
        UserUseCaseException error = assertThrows(UserUseCaseException.class,
                () -> manage.grantPermission(new GrantPermissionCommand("teacher", "course:read", "operator-1", "request-1")));
        assertEquals("PERMISSION_NOT_ACTIVE", error.getCode());
    }

    @Test
    void queries_permissions_for_user() {
        Permission permission = permission(PermissionStatus.ACTIVE);
        when(permissionDomainService.findByUserId(any())).thenReturn(List.of(permission));
        List<PermissionDetailResult> result = manage.getByUser(new GetUserPermissionsQuery(1001L));
        assertEquals(List.of(new PermissionDetailResult("course:read", "Read courses")), result);
    }

    private Role role(RoleStatus status) { return new Role(new RoleCode("teacher"), "Teacher", status); }
    private Permission permission(PermissionStatus status) { return new Permission(new PermissionCode("course:read"), "Read courses", status); }
}
