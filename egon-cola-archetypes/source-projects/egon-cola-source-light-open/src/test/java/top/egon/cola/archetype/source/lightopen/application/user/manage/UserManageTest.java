package top.egon.cola.archetype.source.lightopen.application.user.manage;

import top.egon.cola.archetype.source.lightopen.application.user.command.CreateUserCommand;
import top.egon.cola.archetype.source.lightopen.application.user.convertor.UserApplicationConvertor;
import top.egon.cola.archetype.source.lightopen.application.user.manage.impl.UserManageImpl;
import top.egon.cola.archetype.source.lightopen.application.user.query.GetUserQuery;
import top.egon.cola.archetype.source.lightopen.application.user.result.UserResult;
import top.egon.cola.archetype.source.lightopen.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.lightopen.domain.user.client.UserCachePort;
import top.egon.cola.archetype.source.lightopen.domain.user.entities.User;
import top.egon.cola.archetype.source.lightopen.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.lightopen.domain.user.event.UserEventPublisher;
import top.egon.cola.archetype.source.lightopen.domain.user.exceptions.UserDomainException;
import top.egon.cola.archetype.source.lightopen.domain.user.gateway.UserQueryGateway;
import top.egon.cola.archetype.source.lightopen.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.ExternalUser;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserId;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserSnapshot;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.po.UserPO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManageTest {
    private static final Long USER_ID = 1001L;

    @Mock UserDomainService<UserPO> userDomainService;
    @Mock UserQueryGateway userQueryGateway;
    @Mock UserCachePort userCachePort;
    @Mock UserEventPublisher userEventPublisher;
    @Mock UserApplicationValidator applicationValidator;
    @Mock UserApplicationConvertor convertor;
    @InjectMocks UserManageImpl manage;

    @Test
    void creates_user_through_domain_service() {
        User user = activeUser();
        when(userQueryGateway.findExternalUser("ext-1"))
                .thenReturn(Optional.of(new ExternalUser("ext-1", "Mario")));
        when(userDomainService.createUser("ext-1", "Mario", "mario@example.com"))
                .thenReturn(user);
        when(userDomainService.save(user)).thenReturn(user);
        when(convertor.toResult(any(User.class)))
                .thenReturn(new UserResult(USER_ID, "Mario", "mario@example.com", "ACTIVE"));

        UserResult result = manage.create(command());

        assertEquals("Mario", result.name());
        verify(userCachePort).evictUser(USER_ID);
        verify(userEventPublisher).publish(any());
    }

    @Test
    void rejects_missing_external_user() {
        when(userQueryGateway.findExternalUser("ext-1")).thenReturn(Optional.empty());
        UserUseCaseException error = assertThrows(
                UserUseCaseException.class, () -> manage.create(command()));
        assertEquals("EXTERNAL_USER_NOT_FOUND", error.getCode());
    }

    @Test
    void translates_domain_failure() {
        when(userQueryGateway.findExternalUser("ext-1"))
                .thenReturn(Optional.of(new ExternalUser("ext-1", "Mario")));
        when(userDomainService.createUser("ext-1", "Mario", "mario@example.com"))
                .thenThrow(new UserDomainException("INVALID_USER", "invalid user"));
        UserUseCaseException error = assertThrows(
                UserUseCaseException.class, () -> manage.create(command()));
        assertEquals("INVALID_USER", error.getCode());
    }

    @Test
    void returns_cached_user() {
        UserSnapshot snapshot = new UserSnapshot(USER_ID, "Mario", "mario@example.com", UserStatus.ACTIVE);
        UserResult expected = new UserResult(USER_ID, "Mario", "mario@example.com", "ACTIVE");
        when(userCachePort.getUser(USER_ID)).thenReturn(Optional.of(snapshot));
        when(convertor.toResult(snapshot)).thenReturn(expected);
        assertEquals(expected, manage.get(new GetUserQuery(USER_ID)));
    }

    private CreateUserCommand command() {
        return new CreateUserCommand("ext-1", "Mario", "mario@example.com", "operator-1", "request-1");
    }

    private User activeUser() {
        return new User(new UserId(USER_ID), "ext-1", "Mario", "mario@example.com", UserStatus.ACTIVE);
    }
}
