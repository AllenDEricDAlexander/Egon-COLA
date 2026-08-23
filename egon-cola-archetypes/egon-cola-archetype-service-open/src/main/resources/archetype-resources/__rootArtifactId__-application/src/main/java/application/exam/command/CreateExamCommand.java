#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.command;

import java.time.Instant;

public record CreateExamCommand(
        long courseId, String title, Instant startsAt, Instant endsAt) {
}
