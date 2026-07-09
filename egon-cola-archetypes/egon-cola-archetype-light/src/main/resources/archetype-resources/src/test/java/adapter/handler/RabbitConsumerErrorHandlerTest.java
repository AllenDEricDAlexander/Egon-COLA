package ${package}.adapter.handler;

import ${package}.application.user.manage.UserUseCaseException;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RabbitConsumerErrorHandlerTest {
    private final RabbitConsumerErrorHandler handler = new RabbitConsumerErrorHandler();

    @Test
    void rejects_non_retryable_application_failure() {
        ListenerExecutionFailedException failure = new ListenerExecutionFailedException(
                "failed", new UserUseCaseException("INVALID_IMPORT", "Invalid import"));
        assertThatThrownBy(() -> handler.handleError(null, null, null, failure))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void rethrows_retryable_infrastructure_failure() {
        ListenerExecutionFailedException failure = new ListenerExecutionFailedException(
                "failed", new IllegalStateException("broker temporarily unavailable"));
        assertThatThrownBy(() -> handler.handleError(null, null, null, failure))
                .isSameAs(failure);
    }
}
