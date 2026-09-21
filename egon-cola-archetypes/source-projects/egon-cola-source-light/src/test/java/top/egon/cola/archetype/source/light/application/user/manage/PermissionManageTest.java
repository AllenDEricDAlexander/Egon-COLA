package top.egon.cola.archetype.source.light.application.user.manage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.egon.cola.archetype.source.light.application.user.manage.impl.PermissionManageImpl;
import top.egon.cola.archetype.source.light.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.light.application.user.pojo.convertor.UserApplicationConvertor;
import top.egon.cola.archetype.source.light.application.user.pojo.query.GetUserPermissionsQuery;
import top.egon.cola.archetype.source.light.application.user.pojo.result.PermissionDetailResult;
import top.egon.cola.archetype.source.light.application.user.pojo.result.PermissionResult;
import top.egon.cola.archetype.source.light.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.light.common.exception.UserDomainException;
import top.egon.cola.archetype.source.light.common.exception.UserUseCaseException;
import top.egon.cola.archetype.source.light.domain.user.aggregates.RolePermissionAggregate;
import top.egon.cola.archetype.source.light.domain.user.entities.Permission;
import top.egon.cola.archetype.source.light.domain.user.entities.Role;
import top.egon.cola.archetype.source.light.domain.user.enums.PermissionStatus;
import top.egon.cola.archetype.source.light.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.light.domain.user.service.PermissionDomainService;
import top.egon.cola.archetype.source.light.domain.user.service.RoleDomainService;
import top.egon.cola.archetype.source.light.domain.user.service.UserEventService;
import top.egon.cola.archetype.source.light.domain.user.service.UserIdempotencyService;
import top.egon.cola.archetype.source.light.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.light.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.light.domain.user.vos.UserId;
import top.egon.cola.archetype.source.light.domain.user.vos.UserEvent;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionManageTest {
    @Mock PermissionDomainService permissionDomainService;
    @Mock RoleDomainService roleDomainService;
    @Mock UserEventService userEventService;
    @Mock UserIdempotencyService userIdempotencyService;
    @Mock UserApplicationValidator applicationValidator;
    @Mock UserApplicationConvertor convertor;
    @InjectMocks PermissionManageImpl manage;

    @Test
    void grants_permission_and_persists_aggregate() {
        Role role = role(RoleStatus.ACTIVE);
        Permission permission = permission(PermissionStatus.ACTIVE);
        when(userIdempotencyService.claim("request-1")).thenReturn(true);
        when(roleDomainService.findByCode(new RoleCode("teacher"))).thenReturn(Optional.of(role));
        when(permissionDomainService.findByCode(new PermissionCode("course:read")))
                .thenReturn(Optional.of(permission));
        when(permissionDomainService.grantPermission(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(convertor.toPermissionResult(role, permission))
                .thenReturn(new PermissionResult("teacher", "course:read", "ACTIVE"));

        PermissionResult result = manage.grantPermission(command());

        assertEquals("course:read", result.permissionCode());
        verify(roleDomainService).savePermissions(any(RolePermissionAggregate.class));
        ArgumentCaptor<UserEvent> published = ArgumentCaptor.forClass(UserEvent.class);
        verify(userEventService).publish(published.capture());
        assertEquals("authorization.permission-granted", published.getValue().type());
    }

    @Test
    void rejects_a_replayed_request() {
        when(userIdempotencyService.claim("request-1")).thenReturn(false);

        UserUseCaseException error = assertThrows(UserUseCaseException.class, () -> manage.grantPermission(command()));

        assertEquals("DUPLICATE_REQUEST", error.getStatus());
        verify(roleDomainService, never()).savePermissions(any());
    }

    @Test
    void reports_missing_permission() {
        when(userIdempotencyService.claim("request-1")).thenReturn(true);
        when(roleDomainService.findByCode(new RoleCode("teacher"))).thenReturn(Optional.of(role(RoleStatus.ACTIVE)));
        when(permissionDomainService.findByCode(new PermissionCode("course:read"))).thenReturn(Optional.empty());

        UserUseCaseException error = assertThrows(UserUseCaseException.class, () -> manage.grantPermission(command()));

        assertEquals("PERMISSION_NOT_FOUND", error.getStatus());
    }

    @Test
    void translates_inactive_permission_failure() {
        Role role = role(RoleStatus.ACTIVE);
        Permission permission = permission(PermissionStatus.DISABLED);
        when(userIdempotencyService.claim("request-1")).thenReturn(true);
        when(roleDomainService.findByCode(new RoleCode("teacher"))).thenReturn(Optional.of(role));
        when(permissionDomainService.findByCode(new PermissionCode("course:read")))
                .thenReturn(Optional.of(permission));
        when(permissionDomainService.grantPermission(any(), any()))
                .thenThrow(new UserDomainException("PERMISSION_NOT_ACTIVE", "permission must be active"));

        UserUseCaseException error = assertThrows(UserUseCaseException.class, () -> manage.grantPermission(command()));

        assertEquals("PERMISSION_NOT_ACTIVE", error.getStatus());
    }

    @Test
    void queries_permissions_for_user() {
        Permission permission = permission(PermissionStatus.ACTIVE);
        when(permissionDomainService.findByUserId(new UserId(1001L))).thenReturn(List.of(permission));

        List<PermissionDetailResult> result = manage.getByUser(new GetUserPermissionsQuery(1001L));

        assertEquals(List.of(new PermissionDetailResult("course:read", "Read courses")), result);
    }

    private GrantPermissionCommand command() {
        return new GrantPermissionCommand("teacher", "course:read", "operator-1", "request-1");
    }

    private Role role(RoleStatus status) { return new Role(new RoleCode("teacher"), "Teacher", status); }
    private Permission permission(PermissionStatus status) { return new Permission(new PermissionCode("course:read"), "Read courses", status); }
}
