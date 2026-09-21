package top.egon.cola.archetype.source.light.infrastructure.mq;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.archetype.source.light.domain.teaching.vos.TeachingEvent;
import top.egon.cola.archetype.source.light.facade.teaching.dto.CreateCourseDTO;
import top.egon.cola.archetype.source.light.infrastructure.config.TransactionCompletionExecutor;
import top.egon.cola.archetype.source.light.infrastructure.mq.impl.RabbitMqMessageServiceImpl;

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
    private final RabbitMqMessageServiceImpl service = new RabbitMqMessageServiceImpl(
            rabbitTemplate, new TransactionCompletionExecutor(), environment);

    @Test
    void sends_the_declared_routing_key_only_after_commit() {
        when(environment.getProperty(MqRouteEnum.EXCHANGE_PROPERTY)).thenReturn("sample.domain");
        TeachingEvent event = TeachingEvent.classCreated(1003L);
        TransactionTemplate transaction = transaction("rabbit-mq-commit");

        transaction.executeWithoutResult(status -> service.publish(MqRouteEnum.CLASS_CHANGED, event));

        verify(rabbitTemplate).convertAndSend("sample.domain", "class.changed", event);
    }

    @Test
    void defers_publishing_when_the_transaction_does_not_commit() {
        when(environment.getProperty(MqRouteEnum.EXCHANGE_PROPERTY)).thenReturn("sample.domain");
        TeachingEvent event = TeachingEvent.courseCreated(1002L);
        TransactionTemplate transaction = transaction("rabbit-mq-rollback");

        transaction.executeWithoutResult(status -> {
            service.publish(MqRouteEnum.COURSE_CHANGED, event);
            status.setRollbackOnly();
        });

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void rejects_a_payload_the_route_does_not_accept() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.publish(
                MqRouteEnum.COURSE_IMPORTED, TeachingEvent.courseCreated(1002L)));

        assertEquals("MQ_ROUTE_REJECTED: COURSE_IMPORTED accepts CreateCourseDTO on schema v1",
                error.getMessage());
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void accepts_the_command_payload_declared_by_the_consumer_route() {
        when(environment.getProperty(MqRouteEnum.EXCHANGE_PROPERTY)).thenReturn("sample.domain");
        CreateCourseDTO command = new CreateCourseDTO("math", "Mathematics", "operator-1", "request-1");

        service.publish(MqRouteEnum.COURSE_IMPORTED, command);

        verify(rabbitTemplate).convertAndSend("sample.domain", "course.imported", command);
    }

    private static TransactionTemplate transaction(String name) {
        return new TransactionTemplate(new DataSourceTransactionManager(
                new DriverManagerDataSource("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1", "sa", "")));
    }
}
