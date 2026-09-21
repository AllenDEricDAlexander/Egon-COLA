package top.egon.cola.archetype.source.light.application.user.manage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.egon.cola.archetype.source.light.application.user.manage.impl.UserManageImpl;
import top.egon.cola.archetype.source.light.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.light.application.user.pojo.convertor.UserApplicationConvertor;
import top.egon.cola.archetype.source.light.application.user.pojo.query.GetUserQuery;
import top.egon.cola.archetype.source.light.application.user.pojo.result.UserResult;
import top.egon.cola.archetype.source.light.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.light.common.exception.UserDomainException;
import top.egon.cola.archetype.source.light.common.exception.UserUseCaseException;
import top.egon.cola.archetype.source.light.domain.user.entities.User;
import top.egon.cola.archetype.source.light.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.light.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.light.domain.user.service.UserEventService;
import top.egon.cola.archetype.source.light.domain.user.service.UserIdempotencyService;
import top.egon.cola.archetype.source.light.domain.user.service.UserQueryService;
import top.egon.cola.archetype.source.light.domain.user.vos.ExternalUser;
import top.egon.cola.archetype.source.light.domain.user.vos.UserId;
import top.egon.cola.archetype.source.light.domain.user.vos.UserEvent;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManageTest {
    private static final Long USER_ID = 1001L;

    @Mock UserDomainService userDomainService;
    @Mock UserQueryService userQueryService;
    @Mock UserEventService userEventService;
    @Mock UserIdempotencyService userIdempotencyService;
    @Mock UserApplicationValidator applicationValidator;
    @Mock UserApplicationConvertor convertor;
    @InjectMocks UserManageImpl manage;

    @Test
    void creates_user_through_domain_service() {
        User user = activeUser();
        when(userIdempotencyService.claim("request-1")).thenReturn(true);
        when(userQueryService.findExternalUser("ext-1"))
                .thenReturn(Optional.of(new ExternalUser("ext-1", "Mario")));
        when(userDomainService.createUser("ext-1", "Mario", "mario@example.com"))
                .thenReturn(user);
        when(userDomainService.save(user)).thenReturn(user);
        when(convertor.toTarget(user)).thenReturn(result());

        UserResult result = manage.create(command());

        assertEquals("Mario", result.name());
        verify(applicationValidator).validate(command());
        ArgumentCaptor<UserEvent> published = ArgumentCaptor.forClass(UserEvent.class);
        verify(userEventService).publish(published.capture());
        assertEquals("user.created", published.getValue().type());
        assertEquals(USER_ID, published.getValue().aggregateId());
    }

    @Test
    void rejects_a_replayed_request_before_touching_the_aggregate() {
        when(userIdempotencyService.claim("request-1")).thenReturn(false);

        UserUseCaseException error = assertThrows(
                UserUseCaseException.class, () -> manage.create(command()));

        assertEquals("DUPLICATE_REQUEST", error.getStatus());
        verify(userDomainService, never()).save(any());
    }

    @Test
    void rejects_missing_external_user() {
        when(userIdempotencyService.claim("request-1")).thenReturn(true);
        when(userQueryService.findExternalUser("ext-1")).thenReturn(Optional.empty());

        UserUseCaseException error = assertThrows(
                UserUseCaseException.class, () -> manage.create(command()));

        assertEquals("EXTERNAL_USER_NOT_FOUND", error.getStatus());
    }

    @Test
    void translates_domain_failure() {
        when(userIdempotencyService.claim("request-1")).thenReturn(true);
        when(userQueryService.findExternalUser("ext-1"))
                .thenReturn(Optional.of(new ExternalUser("ext-1", "Mario")));
        when(userDomainService.createUser("ext-1", "Mario", "mario@example.com"))
                .thenThrow(new UserDomainException("INVALID_USER", "invalid user"));

        UserUseCaseException error = assertThrows(
                UserUseCaseException.class, () -> manage.create(command()));

        assertEquals("INVALID_USER", error.getStatus());
    }

    @Test
    void reads_user_through_the_domain_service() {
        User user = activeUser();
        when(userDomainService.findById(new UserId(USER_ID))).thenReturn(Optional.of(user));
        when(convertor.toTarget(user)).thenReturn(result());

        assertEquals(result(), manage.get(new GetUserQuery(USER_ID)));
    }

    @Test
    void reports_missing_user() {
        when(userDomainService.findById(new UserId(USER_ID))).thenReturn(Optional.empty());

        UserUseCaseException error = assertThrows(
                UserUseCaseException.class, () -> manage.get(new GetUserQuery(USER_ID)));

        assertEquals("USER_NOT_FOUND", error.getStatus());
    }

    private CreateUserCommand command() {
        return new CreateUserCommand("ext-1", "Mario", "mario@example.com", "operator-1", "request-1");
    }

    private UserResult result() {
        return new UserResult(USER_ID, "Mario", "mario@example.com", "ACTIVE");
    }

    private User activeUser() {
        return new User(new UserId(USER_ID), "ext-1", "Mario", "mario@example.com", UserStatus.ACTIVE);
    }
}
