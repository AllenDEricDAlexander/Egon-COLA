package ${package}.application.user.manage;

import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.convertor.UserApplicationConvertor;
import ${package}.application.user.manage.impl.UserManageImpl;
import ${package}.application.user.query.GetUserQuery;
import ${package}.application.user.result.UserResult;
import ${package}.application.user.validators.UserApplicationValidator;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.exceptions.UserDomainException;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.user.service.UserCacheService;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.user.service.UserEventPublisher;
import ${package}.domain.user.service.UserQueryService;
import ${package}.domain.user.vos.ExternalUser;
import ${package}.domain.user.vos.UserId;
import ${package}.domain.user.vos.UserSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManageTest {
    private static final String USER_ID = "018f5f9c-4f6a-7c2b-8a1d-123456789ab1";

    @Mock UserDomainService userDomainService;
    @Mock UserRepository userRepository;
    @Mock UserQueryService userQueryService;
    @Mock UserCacheService userCacheService;
    @Mock UserEventPublisher userEventPublisher;
    @Mock UserApplicationValidator applicationValidator;
    @Mock UserApplicationConvertor convertor;
    @InjectMocks UserManageImpl manage;

    @Test
    void creates_user_through_domain_ports() {
        CreateUserCommand command = command();
        when(userQueryService.findExternalUser("ext-1"))
                .thenReturn(Optional.of(new ExternalUser("ext-1", "Mario")));
        when(userDomainService.createUser("ext-1", "Mario", "mario@example.com"))
                .thenReturn(activeUser());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(convertor.toResult(any(User.class)))
                .thenReturn(new UserResult(USER_ID, "Mario", "mario@example.com", "ACTIVE"));

        UserResult result = manage.create(command);

        assertEquals("Mario", result.name());
        verify(userCacheService).evictUser(result.id());
        verify(userEventPublisher).publish(any());
    }

    @Test
    void rejects_missing_external_user() {
        when(userQueryService.findExternalUser("ext-1")).thenReturn(Optional.empty());

        UserUseCaseException error = assertThrows(UserUseCaseException.class, () -> manage.create(command()));

        assertEquals("EXTERNAL_USER_NOT_FOUND", error.getCode());
    }

    @Test
    void translates_domain_failure() {
        when(userQueryService.findExternalUser("ext-1"))
                .thenReturn(Optional.of(new ExternalUser("ext-1", "Mario")));
        when(userDomainService.createUser("ext-1", "Mario", "mario@example.com"))
                .thenThrow(new UserDomainException("INVALID_USER", "invalid user"));

        UserUseCaseException error = assertThrows(UserUseCaseException.class, () -> manage.create(command()));

        assertEquals("INVALID_USER", error.getCode());
    }

    @Test
    void returns_cached_user_without_repository_lookup() {
        UserSnapshot snapshot = new UserSnapshot(USER_ID, "Mario", "mario@example.com", UserStatus.ACTIVE);
        UserResult expected = new UserResult(USER_ID, "Mario", "mario@example.com", "ACTIVE");
        when(userCacheService.getUser(USER_ID)).thenReturn(Optional.of(snapshot));
        when(convertor.toResult(snapshot)).thenReturn(expected);

        UserResult result = manage.get(new GetUserQuery(USER_ID));

        assertEquals(expected, result);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void caches_repository_result_on_query_miss() {
        User user = activeUser();
        UserSnapshot snapshot = UserSnapshot.from(user);
        UserResult expected = new UserResult(USER_ID, "Mario", "mario@example.com", "ACTIVE");
        when(userCacheService.getUser(USER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(new UserId(USER_ID))).thenReturn(Optional.of(user));
        when(convertor.toSnapshot(user)).thenReturn(snapshot);
        when(convertor.toResult(user)).thenReturn(expected);

        UserResult result = manage.get(new GetUserQuery(USER_ID));

        assertEquals(expected, result);
        verify(userCacheService).putUser(snapshot);
    }

    private CreateUserCommand command() {
        return new CreateUserCommand(
                "ext-1", "Mario", "mario@example.com", "operator-1", "request-1");
    }

    private User activeUser() {
        return new User(new UserId(USER_ID), "ext-1", "Mario", "mario@example.com", UserStatus.ACTIVE);
    }
}
