package ${package}.application.user.manage;

import ${package}.application.user.command.AssignRoleCommand;
import ${package}.application.user.convertor.UserApplicationConvertor;
import ${package}.application.user.manage.impl.RoleManageImpl;
import ${package}.application.user.result.UserResult;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.domain.user.aggregates.UserAggregate;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.exceptions.UserDomainException;
import ${package}.domain.user.repos.RoleRepository;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.user.service.RoleDomainService;
import ${package}.domain.user.service.UserCacheService;
import ${package}.domain.user.service.UserEventPublisher;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserId;
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
    @Mock RoleDomainService roleDomainService;
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserCacheService userCacheService;
    @Mock UserEventPublisher userEventPublisher;
    @Mock UserApplicationValidator applicationValidator;
    @Mock UserApplicationConvertor convertor;
    @InjectMocks RoleManageImpl manage;

    @Test
    void assigns_role_and_persists_aggregate() {
        User user = user(UserStatus.ACTIVE);
        Role role = role(RoleStatus.ACTIVE);
        UserAggregate aggregate = new UserAggregate(user);
        aggregate.assign(role);
        when(userRepository.findById(new UserId("u-1"))).thenReturn(Optional.of(user));
        when(roleRepository.findByCode(new RoleCode("teacher"))).thenReturn(Optional.of(role));
        when(roleDomainService.assignRole(any(UserAggregate.class), any(Role.class))).thenReturn(aggregate);
        when(convertor.toResult(user)).thenReturn(new UserResult("u-1", "Mario", "mario@example.com", "ACTIVE"));

        UserResult result = manage.assignRole(command());

        assertEquals("u-1", result.id());
        verify(userRepository).saveRoles(aggregate);
        verify(userEventPublisher).publish(any());
    }

    @Test
    void translates_disabled_user_failure() {
        User user = user(UserStatus.DISABLED);
        Role role = role(RoleStatus.ACTIVE);
        when(userRepository.findById(new UserId("u-1"))).thenReturn(Optional.of(user));
        when(roleRepository.findByCode(new RoleCode("teacher"))).thenReturn(Optional.of(role));
        when(roleDomainService.assignRole(any(), any()))
                .thenThrow(new UserDomainException("USER_NOT_ACTIVE", "user must be active"));

        UserUseCaseException error = assertThrows(UserUseCaseException.class, () -> manage.assignRole(command()));

        assertEquals("USER_NOT_ACTIVE", error.getCode());
    }

    @Test
    void translates_archived_role_failure() {
        User user = user(UserStatus.ACTIVE);
        Role role = role(RoleStatus.ARCHIVED);
        when(userRepository.findById(new UserId("u-1"))).thenReturn(Optional.of(user));
        when(roleRepository.findByCode(new RoleCode("teacher"))).thenReturn(Optional.of(role));
        when(roleDomainService.assignRole(any(), any()))
                .thenThrow(new UserDomainException("ROLE_NOT_ACTIVE", "role must be active"));

        UserUseCaseException error = assertThrows(UserUseCaseException.class, () -> manage.assignRole(command()));

        assertEquals("ROLE_NOT_ACTIVE", error.getCode());
    }

    private AssignRoleCommand command() {
        return new AssignRoleCommand("u-1", "teacher", "operator-1", "request-1");
    }

    private User user(UserStatus status) {
        return new User(new UserId("u-1"), "Mario", "mario@example.com", status);
    }

    private Role role(RoleStatus status) {
        return new Role(new RoleCode("teacher"), "Teacher", status);
    }
}
