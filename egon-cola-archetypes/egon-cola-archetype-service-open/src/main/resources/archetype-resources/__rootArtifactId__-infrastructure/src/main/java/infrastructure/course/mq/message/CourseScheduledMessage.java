#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.mq.message;
import java.time.Instant;
public record CourseScheduledMessage(
        Long scheduleId, Long courseId, Long classId, Instant startsAt, Instant endsAt) { }
