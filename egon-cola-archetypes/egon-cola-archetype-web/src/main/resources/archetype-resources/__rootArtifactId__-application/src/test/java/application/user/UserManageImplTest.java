package ${package}.application.user;

import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.user.assemblers.UserAssembler;
import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.manage.impl.UserManageImpl;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.user.client.UserCachePort;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.user.vos.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManageImplTest {
    @Mock private UserDomainService<?> userDomainService;
    @Mock private UserCachePort userCache;
    @Mock private CommandIdempotencyPort idempotency;
    @Mock private OrganizationEventPublisher eventPublisher;

    @AfterEach void clearContext() { OrganizationRequestContextHolder.clear(); }

    @Test
    void createsUserThroughDomainService() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
                "admin-1", Set.of("ORGANIZATION_ADMIN"), "trace-1"));
        User user = new User(new UserId(2001L), "Mario", "mario@example.com", UserStatus.ACTIVE);
        when(userDomainService.existsByEmail("mario@example.com")).thenReturn(false);
        when(userDomainService.create(any(), any(), any())).thenReturn(user);
        doReturn(user).when(userDomainService).save(any(User.class));
        when(idempotency.claim("create-user", "req-1")).thenReturn(true);
        UserManageImpl manage = new UserManageImpl(userDomainService, new UserApplicationValidator(),
                new UserAssembler(), userCache, idempotency, eventPublisher, () -> 2001L);

        assertEquals(2001L, manage.createUser(
                new CreateUserCommand("req-1", "Mario", "MARIO@EXAMPLE.COM")).id());
        verify(userDomainService).save(user);
    }
}
