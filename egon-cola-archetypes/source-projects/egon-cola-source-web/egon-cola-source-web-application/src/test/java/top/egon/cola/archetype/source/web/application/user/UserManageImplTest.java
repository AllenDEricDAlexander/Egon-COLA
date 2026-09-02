package top.egon.cola.archetype.source.web.application.user;

import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.application.user.assemblers.UserAssembler;
import top.egon.cola.archetype.source.web.application.user.command.CreateUserCommand;
import top.egon.cola.archetype.source.web.application.user.manage.impl.UserManageImpl;
import top.egon.cola.archetype.source.web.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.web.domain.client.CommandIdempotencyPort;
import top.egon.cola.archetype.source.web.domain.client.OrganizationEventPublisher;
import top.egon.cola.archetype.source.web.domain.user.client.UserCachePort;
import top.egon.cola.archetype.source.web.domain.user.entities.User;
import top.egon.cola.archetype.source.web.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.web.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;
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
