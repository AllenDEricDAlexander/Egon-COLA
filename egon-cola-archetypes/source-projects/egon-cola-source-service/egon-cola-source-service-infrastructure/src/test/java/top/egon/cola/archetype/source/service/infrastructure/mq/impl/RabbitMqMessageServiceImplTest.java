package top.egon.cola.archetype.source.service.infrastructure.mq.impl;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.archetype.source.service.infrastructure.course.mq.message.CourseScheduledMessage;
import top.egon.cola.archetype.source.service.infrastructure.mq.MqRouteEnum;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Publish routing, payload acceptance and the commit point all come from the declared route. */
class RabbitMqMessageServiceImplTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final Environment environment = mock(Environment.class);
    private final RabbitMqMessageServiceImpl service =
            new RabbitMqMessageServiceImpl(rabbitTemplate, environment);

    private final CourseScheduledMessage message = new CourseScheduledMessage(
            3001L, 1001L, 2001L, Instant.EPOCH, Instant.EPOCH.plusSeconds(60));

    @Test
    void sendsTheDeclaredRoutingKeyOnlyAfterCommit() {
        stubTargets();
        TransactionTemplate transaction = transaction("rabbit-mq-commit");

        transaction.executeWithoutResult(status -> service.publish(MqRouteEnum.COURSE_SCHEDULED, message));

        verify(rabbitTemplate).convertAndSend("evaluation.events", "course.scheduled", message);
    }

    @Test
    void defersPublishingWhenTheTransactionDoesNotCommit() {
        stubTargets();
        TransactionTemplate transaction = transaction("rabbit-mq-rollback");

        transaction.executeWithoutResult(status -> {
            service.publish(MqRouteEnum.COURSE_SCHEDULED, message);
            status.setRollbackOnly();
        });

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void rejectsAPayloadTheRouteDoesNotAccept() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.publish(MqRouteEnum.COURSE_SCHEDULED, "not-a-message"));

        assertEquals("MQ_ROUTE_REJECTED: COURSE_SCHEDULED accepts class "
                + CourseScheduledMessage.class.getName() + " on schema v1", error.getMessage());
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void rejectsPublishingOnAConsumedOnlyRoute() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.publish(MqRouteEnum.SCORE_COMMAND, message));

        assertEquals("MQ_ROUTE_REJECTED: SCORE_COMMAND accepts null on schema v1", error.getMessage());
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    private void stubTargets() {
        when(environment.getProperty(MqRouteEnum.EXCHANGE_PROPERTY)).thenReturn("evaluation.events");
        when(environment.getProperty(MqRouteEnum.COURSE_SCHEDULED_ROUTING_KEY_PROPERTY))
                .thenReturn("course.scheduled");
    }

    private static TransactionTemplate transaction(String name) {
        return new TransactionTemplate(new DataSourceTransactionManager(
                new DriverManagerDataSource("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1", "sa", "")));
    }
}
