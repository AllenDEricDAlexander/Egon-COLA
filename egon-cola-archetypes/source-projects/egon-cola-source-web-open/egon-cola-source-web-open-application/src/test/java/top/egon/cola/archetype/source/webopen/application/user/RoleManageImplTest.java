package top.egon.cola.archetype.source.webopen.application.user;

import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.webopen.application.user.command.AssignRoleCommand;
import top.egon.cola.archetype.source.webopen.application.user.manage.impl.RoleManageImpl;
import top.egon.cola.archetype.source.webopen.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.webopen.domain.client.CommandIdempotencyPort;
import top.egon.cola.archetype.source.webopen.domain.client.OrganizationEventPublisher;
import top.egon.cola.archetype.source.webopen.domain.user.client.UserCachePort;
import top.egon.cola.archetype.source.webopen.domain.user.entities.Role;
import top.egon.cola.archetype.source.webopen.domain.user.entities.User;
import top.egon.cola.archetype.source.webopen.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.webopen.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.webopen.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.webopen.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;
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
