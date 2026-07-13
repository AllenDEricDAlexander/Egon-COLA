#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.course.dto;

import java.io.Serializable;
import java.time.Instant;

public record CourseScheduleResponse(
        String id,
        String courseId,
        String classId,
        Instant startsAt,
        Instant endsAt,
        String status) implements Serializable {
}
