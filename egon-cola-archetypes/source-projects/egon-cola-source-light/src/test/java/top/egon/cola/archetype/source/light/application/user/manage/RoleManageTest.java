package top.egon.cola.archetype.source.light.application.user.manage;

import top.egon.cola.archetype.source.light.application.user.command.AssignRoleCommand;
import top.egon.cola.archetype.source.light.application.user.convertor.UserApplicationConvertor;
import top.egon.cola.archetype.source.light.application.user.manage.impl.RoleManageImpl;
import top.egon.cola.archetype.source.light.application.user.result.UserResult;
import top.egon.cola.archetype.source.light.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.light.domain.user.client.UserCachePort;
import top.egon.cola.archetype.source.light.domain.user.entities.Role;
import top.egon.cola.archetype.source.light.domain.user.entities.User;
import top.egon.cola.archetype.source.light.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.light.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.light.domain.user.event.UserEventPublisher;
import top.egon.cola.archetype.source.light.domain.user.exceptions.UserDomainException;
import top.egon.cola.archetype.source.light.domain.user.service.RoleDomainService;
import top.egon.cola.archetype.source.light.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.light.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.light.domain.user.vos.UserId;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.po.RolePO;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.po.UserPO;
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
class RoleManageTest {
    @Mock RoleDomainService<RolePO> roleDomainService;
    @Mock UserDomainService<UserPO> userDomainService;
    @Mock UserCachePort userCachePort;
    @Mock UserEventPublisher userEventPublisher;
    @Mock UserApplicationValidator applicationValidator;
    @Mock UserApplicationConvertor convertor;
    @InjectMocks RoleManageImpl manage;

    @Test
    void assigns_role_and_persists_aggregate() {
        User user = user(UserStatus.ACTIVE);
        Role role = role(RoleStatus.ACTIVE);
        when(userDomainService.findById(new UserId(1001L))).thenReturn(Optional.of(user));
        when(roleDomainService.findByCode(new RoleCode("teacher"))).thenReturn(Optional.of(role));
        when(roleDomainService.assignRole(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(convertor.toResult(user)).thenReturn(new UserResult(1001L, "Mario", "mario@example.com", "ACTIVE"));
        UserResult result = manage.assignRole(new AssignRoleCommand(1001L, "teacher", "operator-1", "request-1"));
        assertEquals(1001L, result.id());
        verify(userDomainService).saveRoles(any());
        verify(userEventPublisher).publish(any());
    }

    @Test
    void translates_disabled_user_failure() {
        User user = user(UserStatus.DISABLED);
        Role role = role(RoleStatus.ACTIVE);
        when(userDomainService.findById(new UserId(1001L))).thenReturn(Optional.of(user));
        when(roleDomainService.findByCode(new RoleCode("teacher"))).thenReturn(Optional.of(role));
        when(roleDomainService.assignRole(any(), any()))
                .thenThrow(new UserDomainException("USER_NOT_ACTIVE", "user must be active"));
        UserUseCaseException error = assertThrows(UserUseCaseException.class,
                () -> manage.assignRole(new AssignRoleCommand(1001L, "teacher", "operator-1", "request-1")));
        assertEquals("USER_NOT_ACTIVE", error.getCode());
    }

    private User user(UserStatus status) {
        return new User(new UserId(1001L), "ext-1", "Mario", "mario@example.com", status);
    }

    private Role role(RoleStatus status) {
        return new Role(new RoleCode("teacher"), "Teacher", status);
    }
}
