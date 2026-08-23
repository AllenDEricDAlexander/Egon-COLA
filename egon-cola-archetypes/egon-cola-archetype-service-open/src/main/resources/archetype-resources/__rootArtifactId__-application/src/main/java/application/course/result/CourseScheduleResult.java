#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.course.result;

import java.time.Instant;

public record CourseScheduleResult(
        String id,
        String courseId,
        String classId,
        Instant startsAt,
        Instant endsAt,
        String status) {
}
