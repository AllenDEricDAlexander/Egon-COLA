package ${package}.application.user;

import ${package}.application.user.command.AssignRoleCommand;
import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.user.manage.impl.RoleManageImpl;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.repos.RoleRepository;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.user.client.UserCachePort;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleManageImplTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserCachePort userCache;
    @Mock CommandIdempotencyPort idempotency;
    @Mock OrganizationEventPublisher eventPublisher;

    @AfterEach
    void clearContext() { OrganizationRequestContextHolder.clear(); }

    @Test
    void assignsNormalizedRoleThroughAggregate() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
            "admin-1", Set.of("ORGANIZATION_ADMIN"), "trace-1"));
        when(userRepository.findById(new UserId("u-1"))).thenReturn(Optional.of(
            new User(new UserId("u-1"), "Mario", "mario@example.com", UserStatus.ACTIVE, List.of())));
        when(roleRepository.findByCode(new RoleCode("STUDENT"))).thenReturn(Optional.of(
            new Role("role-student", new RoleCode("STUDENT"), "Student", RoleStatus.ACTIVE)));
        RoleManageImpl manage = new RoleManageImpl(
            userRepository, roleRepository, new UserApplicationValidator(), userCache, idempotency,
            eventPublisher);
        when(idempotency.claim("assign-role", "req-role")).thenReturn(true);

        manage.assignRole(new AssignRoleCommand("req-role", "u-1", "student"));

        verify(userRepository).save(argThat(
            saved -> saved.roleCodes().contains(new RoleCode("STUDENT"))));
    }
}
