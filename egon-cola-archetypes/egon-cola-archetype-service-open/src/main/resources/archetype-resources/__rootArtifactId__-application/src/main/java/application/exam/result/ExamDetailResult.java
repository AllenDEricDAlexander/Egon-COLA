#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.result;

import java.time.Instant;

public record ExamDetailResult(
        long id,
        long courseId,
        String title,
        Instant startsAt,
        Instant endsAt,
        String status) {
}
