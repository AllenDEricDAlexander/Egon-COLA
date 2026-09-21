package top.egon.cola.archetype.source.lightopen.application.user.manage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.egon.cola.archetype.source.lightopen.application.user.manage.impl.RoleManageImpl;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.command.AssignRoleCommand;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.convertor.UserApplicationConvertor;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.result.UserResult;
import top.egon.cola.archetype.source.lightopen.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.lightopen.common.exception.UserDomainException;
import top.egon.cola.archetype.source.lightopen.common.exception.UserUseCaseException;
import top.egon.cola.archetype.source.lightopen.domain.user.aggregates.UserAggregate;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.User;
import top.egon.cola.archetype.source.lightopen.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.lightopen.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.lightopen.domain.user.service.RoleDomainService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserEventService;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserIdempotencyService;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserId;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserEvent;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleManageTest {
    @Mock RoleDomainService roleDomainService;
    @Mock UserDomainService userDomainService;
    @Mock UserEventService userEventService;
    @Mock UserIdempotencyService userIdempotencyService;
    @Mock UserApplicationValidator applicationValidator;
    @Mock UserApplicationConvertor convertor;
    @InjectMocks RoleManageImpl manage;

    @Test
    void assigns_role_and_persists_aggregate() {
        User user = user(UserStatus.ACTIVE);
        Role role = role(RoleStatus.ACTIVE);
        when(userIdempotencyService.claim("request-1")).thenReturn(true);
        when(userDomainService.findById(new UserId(1001L))).thenReturn(Optional.of(user));
        when(roleDomainService.findByCode(new RoleCode("teacher"))).thenReturn(Optional.of(role));
        when(roleDomainService.assignRole(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(convertor.toTarget(user)).thenReturn(new UserResult(1001L, "Mario", "mario@example.com", "ACTIVE"));

        UserResult result = manage.assignRole(command());

        assertEquals(1001L, result.id());
        verify(userDomainService).saveRoles(any(UserAggregate.class));
        ArgumentCaptor<UserEvent> published = ArgumentCaptor.forClass(UserEvent.class);
        verify(userEventService).publish(published.capture());
        assertEquals("user.role-assigned", published.getValue().type());
    }

    @Test
    void rejects_a_replayed_request() {
        when(userIdempotencyService.claim("request-1")).thenReturn(false);

        UserUseCaseException error = assertThrows(UserUseCaseException.class, () -> manage.assignRole(command()));

        assertEquals("DUPLICATE_REQUEST", error.getStatus());
        verify(userDomainService, never()).saveRoles(any());
    }

    @Test
    void reports_missing_role() {
        when(userIdempotencyService.claim("request-1")).thenReturn(true);
        when(userDomainService.findById(new UserId(1001L))).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        when(roleDomainService.findByCode(new RoleCode("teacher"))).thenReturn(Optional.empty());

        UserUseCaseException error = assertThrows(UserUseCaseException.class, () -> manage.assignRole(command()));

        assertEquals("ROLE_NOT_FOUND", error.getStatus());
    }

    @Test
    void translates_disabled_user_failure() {
        User user = user(UserStatus.DISABLED);
        Role role = role(RoleStatus.ACTIVE);
        when(userIdempotencyService.claim("request-1")).thenReturn(true);
        when(userDomainService.findById(new UserId(1001L))).thenReturn(Optional.of(user));
        when(roleDomainService.findByCode(new RoleCode("teacher"))).thenReturn(Optional.of(role));
        when(roleDomainService.assignRole(any(), any()))
                .thenThrow(new UserDomainException("USER_NOT_ACTIVE", "user must be active"));

        UserUseCaseException error = assertThrows(UserUseCaseException.class, () -> manage.assignRole(command()));

        assertEquals("USER_NOT_ACTIVE", error.getStatus());
    }

    private AssignRoleCommand command() {
        return new AssignRoleCommand(1001L, "teacher", "operator-1", "request-1");
    }

    private User user(UserStatus status) {
        return new User(new UserId(1001L), "ext-1", "Mario", "mario@example.com", status);
    }

    private Role role(RoleStatus status) {
        return new Role(new RoleCode("teacher"), "Teacher", status);
    }
}
