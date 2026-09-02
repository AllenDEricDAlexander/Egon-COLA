package top.egon.cola.archetype.source.webopen.adapter;

import top.egon.cola.archetype.source.webopen.adapter.user.dto.CreateUserMessage;
import top.egon.cola.archetype.source.webopen.adapter.mq.RetryableOrganizationMessageException;
import top.egon.cola.archetype.source.webopen.adapter.user.mq.UserCreatedConsumer;
import top.egon.cola.archetype.source.webopen.application.user.command.CreateUserCommand;
import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationFailureType;
import top.egon.cola.archetype.source.webopen.application.user.manage.UserManage;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.archetype.source.webopen.adapter.mq.OrganizationMessageSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizationRabbitMqConsumerTest {

    @AfterEach
    void clearContext() {
        OrganizationRequestContextHolder.clear();
    }

    @Test
    void createUserMessageDelegatesToTheSharedCommand() {
        UserManage userManage = mock(UserManage.class);
        UserCreatedConsumer consumer = new UserCreatedConsumer(userManage,
            new OrganizationMessageSupport((LongIdGenerator) () -> 9001L));

        consumer.consume(new CreateUserMessage("req-1", "Mario", "mario@example.com"));

        verify(userManage).createUser(new CreateUserCommand("req-1", "Mario", "mario@example.com"));
        assertThat(OrganizationRequestContextHolder.current()).isEmpty();
    }

    @Test
    void duplicateCommandIsAcknowledgedWithoutSecondMutation() {
        UserManage userManage = mock(UserManage.class);
        when(userManage.createUser(any())).thenThrow(new OrganizationApplicationException(
                OrganizationFailureType.CONFLICT, "ORG_CONFLICT", "duplicate"));
        UserCreatedConsumer consumer = new UserCreatedConsumer(userManage,
            new OrganizationMessageSupport((LongIdGenerator) () -> 9001L));

        assertThatCode(() -> consumer.consume(
                new CreateUserMessage("req-1", "Mario", "mario@example.com"))).doesNotThrowAnyException();
        verify(userManage, times(1)).createUser(any());
    }

    @Test
    void dependencyFailureRemainsRetryable() {
        UserManage userManage = mock(UserManage.class);
        when(userManage.createUser(any())).thenThrow(new OrganizationApplicationException(
                OrganizationFailureType.DEPENDENCY_UNAVAILABLE,
                "ORG_DEPENDENCY_UNAVAILABLE", "db"));
        UserCreatedConsumer consumer = new UserCreatedConsumer(userManage,
            new OrganizationMessageSupport((LongIdGenerator) () -> 9001L));

        assertThatThrownBy(() -> consumer.consume(
                new CreateUserMessage("req-1", "Mario", "mario@example.com")))
                .isInstanceOf(RetryableOrganizationMessageException.class);
    }
}
