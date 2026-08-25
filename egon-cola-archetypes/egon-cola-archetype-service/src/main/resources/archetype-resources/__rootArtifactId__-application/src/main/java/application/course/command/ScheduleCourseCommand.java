#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.course.command;

import java.time.Instant;

public record ScheduleCourseCommand(
        Long courseId, Long classId, Instant startsAt, Instant endsAt) {
}
