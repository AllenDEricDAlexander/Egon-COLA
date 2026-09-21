package top.egon.cola.archetype.source.web.application.user;

import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.application.user.pojo.convertor.UserConverter;
import top.egon.cola.archetype.source.web.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.web.application.user.manage.impl.UserManageImpl;
import top.egon.cola.archetype.source.web.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.web.domain.service.CommandIdempotencyService;
import top.egon.cola.archetype.source.web.domain.service.OrganizationEventService;
import top.egon.cola.archetype.source.web.domain.user.entities.User;
import top.egon.cola.archetype.source.web.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.web.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import org.mapstruct.factory.Mappers;
import top.egon.cola.component.common.core.validation.ValidationUtils;

@ExtendWith(MockitoExtension.class)
class UserManageImplTest {
    @Mock ValidationUtils validationUtils;

    private UserApplicationValidator userValidator() {
        return new UserApplicationValidator(validationUtils);
    }

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    @Mock private UserDomainService userDomainService;
    @Mock private CommandIdempotencyService idempotency;
    @Mock private OrganizationEventService eventPublisher;

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
        UserManageImpl manage = new UserManageImpl(userDomainService, userValidator(),
                Mappers.getMapper(UserConverter.class), idempotency, eventPublisher);

        assertEquals(2001L, manage.createUser(
                new CreateUserCommand("req-1", "Mario", "MARIO@EXAMPLE.COM")).id());
        verify(userDomainService).save(user);
    }
}
