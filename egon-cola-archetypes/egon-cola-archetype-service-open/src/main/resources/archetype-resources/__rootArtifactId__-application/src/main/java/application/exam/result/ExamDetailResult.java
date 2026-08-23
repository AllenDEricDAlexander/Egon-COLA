#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.result;

import java.time.Instant;

public record ExamDetailResult(
        String id,
        String courseId,
        String title,
        Instant startsAt,
        Instant endsAt,
        String status) {
}
