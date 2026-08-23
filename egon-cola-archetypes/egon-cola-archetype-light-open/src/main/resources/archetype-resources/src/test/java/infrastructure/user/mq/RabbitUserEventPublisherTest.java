package ${package}.infrastructure.user.mq;

import ${package}.domain.user.vos.UserEvent;
import ${package}.infrastructure.config.TransactionCompletionExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RabbitUserEventPublisherTest {
    @Test
    void publishes_user_and_authorization_routes_only_after_commit() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitUserEventPublisher publisher = new RabbitUserEventPublisher(
                rabbitTemplate, new TransactionCompletionExecutor(), "sample.domain");
        TransactionTemplate transaction = transactionTemplate();
        UserEvent created = UserEvent.created("u-1");

        transaction.executeWithoutResult(status -> {
            publisher.publish(created);
            verify(rabbitTemplate, never()).convertAndSend("sample.domain", "user.changed", created);
        });
        verify(rabbitTemplate).convertAndSend("sample.domain", "user.changed", created);

        UserEvent authorization = UserEvent.permissionGranted("teacher");
        transaction.executeWithoutResult(status -> publisher.publish(authorization));
        verify(rabbitTemplate).convertAndSend("sample.domain", "authorization.changed", authorization);

        UserEvent rolledBack = UserEvent.created("u-2");
        transaction.executeWithoutResult(status -> {
            publisher.publish(rolledBack);
            status.setRollbackOnly();
        });
        verify(rabbitTemplate, never()).convertAndSend("sample.domain", "user.changed", rolledBack);
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(new DataSourceTransactionManager(new DriverManagerDataSource(
                "jdbc:h2:mem:rabbit-user;DB_CLOSE_DELAY=-1", "sa", "")));
    }
}
