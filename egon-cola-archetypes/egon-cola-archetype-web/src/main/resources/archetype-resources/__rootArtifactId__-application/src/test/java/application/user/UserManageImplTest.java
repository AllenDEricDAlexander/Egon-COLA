package ${package}.application.user;

import ${package}.application.assemblers.user.UserAssembler;
import ${package}.application.command.user.CreateUserCommand;
import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.manage.user.impl.UserManageImpl;
import ${package}.application.result.user.UserDetailResult;
import ${package}.application.validators.user.UserApplicationValidator;
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

import java.util.Set;

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
            userCache, idempotency, eventPublisher);

        UserDetailResult result = manage.createUser(
            new CreateUserCommand("req-1", "Mario", "MARIO@EXAMPLE.COM"));

        assertEquals("mario@example.com", result.email());
        verify(userRepository).save(any(User.class));
    }
}
