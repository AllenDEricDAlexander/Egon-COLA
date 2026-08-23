package ${package}.adapter;

import ${package}.adapter.user.dto.CreateUserMessage;
import ${package}.adapter.mq.RetryableOrganizationMessageException;
import ${package}.adapter.user.mq.UserCreatedConsumer;
import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
import ${package}.application.user.manage.UserManage;
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
        UserCreatedConsumer consumer = new UserCreatedConsumer(userManage);

        consumer.consume(new CreateUserMessage("req-1", "Mario", "mario@example.com"));

        verify(userManage).createUser(new CreateUserCommand("req-1", "Mario", "mario@example.com"));
        assertThat(OrganizationRequestContextHolder.current()).isEmpty();
    }

    @Test
    void duplicateCommandIsAcknowledgedWithoutSecondMutation() {
        UserManage userManage = mock(UserManage.class);
        when(userManage.createUser(any())).thenThrow(new OrganizationApplicationException(
                OrganizationFailureType.CONFLICT, "ORG_CONFLICT", "duplicate"));
        UserCreatedConsumer consumer = new UserCreatedConsumer(userManage);

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
        UserCreatedConsumer consumer = new UserCreatedConsumer(userManage);

        assertThatThrownBy(() -> consumer.consume(
                new CreateUserMessage("req-1", "Mario", "mario@example.com")))
                .isInstanceOf(RetryableOrganizationMessageException.class);
    }
}
