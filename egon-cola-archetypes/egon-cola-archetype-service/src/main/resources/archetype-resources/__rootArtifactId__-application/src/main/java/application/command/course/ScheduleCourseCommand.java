#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.command.course;

import java.time.Instant;

public record ScheduleCourseCommand(
        String courseId, String classId, Instant startsAt, Instant endsAt) {
}
