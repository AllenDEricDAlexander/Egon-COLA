package ${package}.application.user;

import ${package}.application.user.assemblers.UserAssembler;
import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.user.manage.impl.UserManageImpl;
import ${package}.application.user.result.UserDetailResult;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.user.client.UserCachePort;
import ${package}.domain.user.service.UserDomainService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.egon.cola.component.common.id.generator.UuidV7Generator;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManageImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock private UserCachePort userCache;
    @Mock private CommandIdempotencyPort idempotency;
    @Mock private OrganizationEventPublisher eventPublisher;

    private final UserDomainService userDomainService = new ${package}.domain.user.service.impl.UserDomainServiceImpl();

    @AfterEach
    void clearContext() {
        OrganizationRequestContextHolder.clear();
    }

    @Test
    void createsUserThroughDomainAndRepository() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
            "admin-1", Set.of("ORGANIZATION_ADMIN"), "trace-1"));
        when(userRepository.existsByEmail("mario@example.com")).thenReturn(false);
        when(idempotency.claim("create-user", "req-1")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserManageImpl manage = new UserManageImpl(
            userRepository, userDomainService, new UserApplicationValidator(), new UserAssembler(),
            userCache, idempotency, eventPublisher, new UuidV7Generator());

        UserDetailResult result = manage.createUser(
            new CreateUserCommand("req-1", "Mario", "MARIO@EXAMPLE.COM"));

        assertEquals("mario@example.com", result.email());
        assertEquals(7, UUID.fromString(result.id()).version());
        assertEquals(36, result.id().length());
        verify(userRepository).save(any(User.class));
    }
}
