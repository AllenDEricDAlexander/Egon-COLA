#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.mq.course;
import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.event.CourseEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(prefix = "app.integrations.rabbitmq", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalCourseEventPublisher implements CourseEventPublisher {
    public void courseScheduled(CourseSchedule schedule) { }
}
