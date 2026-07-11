#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.command.exam;

import java.time.Instant;

public record CreateExamCommand(
        String courseId, String title, Instant startsAt, Instant endsAt) {
}
