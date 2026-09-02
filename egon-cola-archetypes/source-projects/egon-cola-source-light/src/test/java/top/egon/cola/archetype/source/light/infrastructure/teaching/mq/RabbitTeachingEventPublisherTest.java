package top.egon.cola.archetype.source.light.infrastructure.teaching.mq;

import top.egon.cola.archetype.source.light.domain.teaching.vos.TeachingEvent;
import top.egon.cola.archetype.source.light.infrastructure.config.TransactionCompletionExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitTeachingEventPublisherTest {
    @Test
    void publishes_class_course_and_schedule_routes() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitTeachingEventPublisher publisher = new RabbitTeachingEventPublisher(
                rabbitTemplate, new TransactionCompletionExecutor(), "sample.domain");
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(
                new DriverManagerDataSource("jdbc:h2:mem:rabbit-teaching;DB_CLOSE_DELAY=-1", "sa", "")));
        TeachingEvent classEvent = TeachingEvent.classCreated(1003L);
        TeachingEvent courseEvent = TeachingEvent.courseCreated(1002L);
        TeachingEvent scheduleEvent = TeachingEvent.courseScheduled(1003L);

        transaction.executeWithoutResult(status -> publisher.publish(classEvent));
        transaction.executeWithoutResult(status -> publisher.publish(courseEvent));
        transaction.executeWithoutResult(status -> publisher.publish(scheduleEvent));

        verify(rabbitTemplate).convertAndSend("sample.domain", "class.changed", classEvent);
        verify(rabbitTemplate).convertAndSend("sample.domain", "course.changed", courseEvent);
        verify(rabbitTemplate).convertAndSend("sample.domain", "schedule.changed", scheduleEvent);
    }
}
