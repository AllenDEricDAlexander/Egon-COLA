#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.course.result;

import java.time.Instant;

public record CourseScheduleResult(
        Long id,
        Long courseId,
        Long classId,
        Instant startsAt,
        Instant endsAt,
        String status) {
}
