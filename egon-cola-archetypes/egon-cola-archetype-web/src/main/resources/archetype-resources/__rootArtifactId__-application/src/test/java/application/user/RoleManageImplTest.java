package ${package}.application.user;

import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.user.command.AssignRoleCommand;
import ${package}.application.user.manage.impl.RoleManageImpl;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.user.client.UserCachePort;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.service.UserDomainService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleManageImplTest {
    @Mock UserDomainService<?> userDomainService;
    @Mock UserCachePort userCache;
    @Mock CommandIdempotencyPort idempotency;
    @Mock OrganizationEventPublisher eventPublisher;

    @AfterEach void clearContext() { OrganizationRequestContextHolder.clear(); }

    @Test
    void assignsNormalizedRoleThroughAggregate() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
                "admin-1", Set.of("ORGANIZATION_ADMIN"), "trace-1"));
        User user = new User(new UserId(1001L), "Mario", "mario@example.com", UserStatus.ACTIVE, List.of());
        Role role = new Role(2001L, new RoleCode("STUDENT"), "Student", RoleStatus.ACTIVE);
        when(userDomainService.findById(new UserId(1001L))).thenReturn(Optional.of(user));
        when(userDomainService.findRoleByCode(new RoleCode("STUDENT"))).thenReturn(Optional.of(role));
        when(idempotency.claim("assign-role", "req-role")).thenReturn(true);
        RoleManageImpl manage = new RoleManageImpl(userDomainService, new UserApplicationValidator(),
                userCache, idempotency, eventPublisher, () -> 2001L);

        manage.assignRole(new AssignRoleCommand("req-role", 1001L, "student"));

        verify(userDomainService).save(user);
    }
}
