package top.egon.cola.archetype.source.serviceopen.infrastructure.course.mq;
import top.egon.cola.archetype.source.serviceopen.domain.course.entities.CourseSchedule;
import top.egon.cola.archetype.source.serviceopen.domain.course.event.CourseEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(prefix = "app.integrations.rabbitmq", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalCourseEventPublisher implements CourseEventPublisher {
    public void courseScheduled(CourseSchedule schedule) { }
}
